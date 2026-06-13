package tool.rental.api.controllers;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.annotations.CustomerOnly;
import tool.rental.api.annotations.RequireAuth;
import tool.rental.api.entities.ItemRental;
import tool.rental.api.entities.ItemRentalUpdate;
import tool.rental.api.entities.QItemRental;
import tool.rental.api.entities.RentalStatus;
import tool.rental.api.entities.User;
import tool.rental.api.repositories.ItemRentalRepository;
import tool.rental.api.repositories.ItemRentalUpdateRepository;
import tool.rental.api.repositories.ItemRepository;
import tool.rental.api.services.AppUserDetails;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.time.LocalDate;

@RestController
@RequestMapping("/rentals")
public class ItemRentalController {

    private final ItemRentalRepository itemRentalRepository;
    private final ItemRentalUpdateRepository itemRentalUpdateRepository;
    private final ItemRepository itemRepository;

    public ItemRentalController(ItemRentalRepository itemRentalRepository,
                                ItemRentalUpdateRepository itemRentalUpdateRepository,
                                ItemRepository itemRepository) {
        this.itemRentalRepository = itemRentalRepository;
        this.itemRentalUpdateRepository = itemRentalUpdateRepository;
        this.itemRepository = itemRepository;
    }

    private void recordUpdate(ItemRental rental, User actor) {
        var entry = new ItemRentalUpdate();
        entry.setRental(rental);
        entry.setStatus(rental.getStatus());
        entry.setCreatedBy(actor);
        itemRentalUpdateRepository.save(entry);
    }

    static Predicate rentalPredicate(Long userId, String s, List<RentalStatus> statuses) {
        var r = QItemRental.itemRental;
        var pred = new BooleanBuilder();
        if (userId != null)
            pred.and(r.user.id.eq(userId));
        if (s != null && !s.isBlank())
            pred.and(r.item.name.containsIgnoreCase(s)
                    .or(r.item.producer.containsIgnoreCase(s))
                    .or(r.item.description.containsIgnoreCase(s)));
        if (statuses != null && !statuses.isEmpty())
            pred.and(r.status.in(statuses));
        return pred;
    }

    @AdminOnly
    @GetMapping
    public Page<ItemRental> index(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String s,
            @RequestParam(required = false) List<RentalStatus> statuses) {
        return itemRentalRepository.findAll(rentalPredicate(null, s, statuses), pageable);
    }

    @RequireAuth
    @GetMapping("/me")
    public Page<ItemRental> mine(
            @AuthenticationPrincipal AppUserDetails principal,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String s,
            @RequestParam(required = false) List<RentalStatus> statuses) {
        return itemRentalRepository.findAll(rentalPredicate(principal.user().getId(), s, statuses), pageable);
    }

    @RequireAuth
    @GetMapping("/{id}")
    public ResponseEntity<ItemRental> getOne(@AuthenticationPrincipal AppUserDetails principal,
                                             @PathVariable Long id) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        if (!principal.canAccess(rental)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(rental);
    }

    record CreateRentalRequest(Long itemId, LocalDate startDate, Integer amount) {}

    @Transactional
    @CustomerOnly
    @PostMapping
    public ResponseEntity<ItemRental> create(@AuthenticationPrincipal AppUserDetails principal,
                                             @RequestBody CreateRentalRequest req) {
        if (req.itemId() == null || req.startDate() == null || req.amount() == null || req.amount() < 1) {
            return ResponseEntity.badRequest().build();
        }
        var item = itemRepository.findByIdForUpdate(req.itemId()).orElse(null);
        if (item == null) return ResponseEntity.badRequest().build();
        if (item.getAvailableAmount() == null || req.amount() > item.getAvailableAmount()) {
            return ResponseEntity.badRequest().build();
        }

        var rental = new ItemRental();
        rental.setItem(item);
        rental.setUser(principal.user());
        rental.setStartDate(req.startDate());
        rental.setDueDate(req.startDate().plusDays(14));
        rental.setAmount(req.amount());

        var saved = itemRentalRepository.save(rental);
        recordUpdate(saved, principal.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    record UpdateRentalRequest(RentalStatus status) {}

    @AdminOnly
    @PatchMapping("/{id}")
    public ResponseEntity<ItemRental> update(@AuthenticationPrincipal AppUserDetails principal,
                                             @PathVariable Long id,
                                             @RequestBody UpdateRentalRequest req) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        if (req.status() == null || req.status() == rental.getStatus()) return ResponseEntity.ok(rental);
        rental.setStatus(req.status());
        var saved = itemRentalRepository.save(rental);
        recordUpdate(saved, principal.user());
        return ResponseEntity.ok(saved);
    }

    @CustomerOnly
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ItemRental> cancel(@AuthenticationPrincipal AppUserDetails principal,
                                             @PathVariable Long id) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        if (!rental.getUser().getId().equals(principal.user().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (rental.getStatus() != RentalStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        rental.setStatus(RentalStatus.CANCELLED);
        var saved = itemRentalRepository.save(rental);
        recordUpdate(saved, principal.user());
        return ResponseEntity.ok(saved);
    }

    @RequireAuth
    @GetMapping("/{id}/updates")
    public ResponseEntity<List<ItemRentalUpdate>> updates(@AuthenticationPrincipal AppUserDetails principal,
                                                          @PathVariable Long id) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        if (!principal.canAccess(rental)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(itemRentalUpdateRepository.findByRentalIdOrderByCreatedAtAsc(id));
    }
}
