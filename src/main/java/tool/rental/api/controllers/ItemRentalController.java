package tool.rental.api.controllers;

import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.annotations.CustomerOnly;
import tool.rental.api.annotations.RequireAuth;
import tool.rental.api.entities.ItemRental;
import tool.rental.api.entities.ItemRentalUpdate;
import tool.rental.api.entities.RentalStatus;
import tool.rental.api.entities.Role;
import tool.rental.api.entities.User;
import tool.rental.api.repositories.ItemRentalRepository;
import tool.rental.api.repositories.ItemRentalUpdateRepository;
import tool.rental.api.repositories.ItemRepository;
import tool.rental.api.repositories.UserRepository;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

@RestController
@RequestMapping("/rentals")
public class ItemRentalController {

    private final ItemRentalRepository itemRentalRepository;
    private final ItemRentalUpdateRepository itemRentalUpdateRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ItemRentalController(ItemRentalRepository itemRentalRepository,
                                ItemRentalUpdateRepository itemRentalUpdateRepository,
                                ItemRepository itemRepository,
                                UserRepository userRepository) {
        this.itemRentalRepository = itemRentalRepository;
        this.itemRentalUpdateRepository = itemRentalUpdateRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    private void recordUpdate(ItemRental rental, User actor) {
        var entry = new ItemRentalUpdate();
        entry.setRental(rental);
        entry.setStatus(rental.getStatus());
        entry.setCreatedBy(actor);
        itemRentalUpdateRepository.save(entry);
    }

    static Pageable pageable(int page, int pageSize, String sort, String direction) {
        Sort sorting = (sort != null && !sort.isBlank())
                ? Sort.by("desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sort)
                : Sort.unsorted();
        return PageRequest.of(page, pageSize, sorting);
    }

    @AdminOnly
    @GetMapping
    public Page<ItemRental> index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String s,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        var pageable = pageable(page, pageSize, sort, direction);
        return (s != null && !s.isBlank())
                ? itemRentalRepository.search(s, pageable)
                : itemRentalRepository.findAll(pageable);
    }

    @RequireAuth
    @GetMapping("/me")
    public ResponseEntity<Page<ItemRental>> mine(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String s,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        var user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        var pageable = pageable(page, pageSize, sort, direction);
        var result = (s != null && !s.isBlank())
                ? itemRentalRepository.searchByUser(user.getId(), s, pageable)
                : itemRentalRepository.findByUser(user.getId(), pageable);
        return ResponseEntity.ok(result);
    }

    @RequireAuth
    @GetMapping("/{id}")
    public ResponseEntity<ItemRental> getOne(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long id) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        var caller = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (caller == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (caller.getRole() != Role.ADMIN && !rental.getUser().getId().equals(caller.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(rental);
    }

    record CreateRentalRequest(Long itemId, LocalDate startDate, Integer amount) {}

    @CustomerOnly
    @PostMapping
    public ResponseEntity<ItemRental> create(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody CreateRentalRequest req) {
        if (req.itemId() == null || req.startDate() == null || req.amount() == null || req.amount() < 1) {
            return ResponseEntity.badRequest().build();
        }
        var item = itemRepository.findById(req.itemId()).orElse(null);
        var user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (item == null || user == null) return ResponseEntity.badRequest().build();
        if (item.getAvailableAmount() == null || req.amount() > item.getAvailableAmount()) {
            return ResponseEntity.badRequest().build();
        }

        var rental = new ItemRental();
        rental.setItem(item);
        rental.setUser(user);
        rental.setStartDate(req.startDate());
        rental.setDueDate(req.startDate().plusDays(14));
        rental.setAmount(req.amount());

        var saved = itemRentalRepository.save(rental);
        recordUpdate(saved, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    record UpdateRentalRequest(RentalStatus status) {}

    @AdminOnly
    @PatchMapping("/{id}")
    public ResponseEntity<ItemRental> update(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long id,
                                             @RequestBody UpdateRentalRequest req) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        var actor = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        boolean statusChanged = req.status() != null && req.status() != rental.getStatus();
        if (statusChanged) rental.setStatus(req.status());
        var saved = itemRentalRepository.save(rental);
        if (statusChanged) recordUpdate(saved, actor);
        return ResponseEntity.ok(saved);
    }

    @CustomerOnly
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ItemRental> cancel(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long id) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        var caller = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (caller == null || !rental.getUser().getId().equals(caller.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (rental.getStatus() != RentalStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        rental.setStatus(RentalStatus.CANCELLED);
        var saved = itemRentalRepository.save(rental);
        recordUpdate(saved, caller);
        return ResponseEntity.ok(saved);
    }

    @RequireAuth
    @GetMapping("/{id}/updates")
    public ResponseEntity<List<ItemRentalUpdate>> updates(@AuthenticationPrincipal UserDetails userDetails,
                                                          @PathVariable Long id) {
        var rental = itemRentalRepository.findById(id).orElse(null);
        if (rental == null) return ResponseEntity.notFound().build();
        var caller = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (caller == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (caller.getRole() != Role.ADMIN && !rental.getUser().getId().equals(caller.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(itemRentalUpdateRepository.findByRentalIdOrderByCreatedAtAsc(id));
    }
}
