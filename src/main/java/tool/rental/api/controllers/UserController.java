package tool.rental.api.controllers;

import com.querydsl.core.BooleanBuilder;
import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.annotations.RequireAuth;
import tool.rental.api.entities.Address;
import tool.rental.api.entities.ItemRental;
import tool.rental.api.entities.QUser;
import tool.rental.api.entities.Role;
import tool.rental.api.entities.User;
import tool.rental.api.repositories.ItemRentalRepository;
import tool.rental.api.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final ItemRentalRepository itemRentalRepository;

    public UserController(UserRepository userRepository, ItemRentalRepository itemRentalRepository) {
        this.userRepository = userRepository;
        this.itemRentalRepository = itemRentalRepository;
    }

    record UpdateProfileRequest(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            Address address
    ) {}

    @AdminOnly
    @GetMapping
    public Page<User> index(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String s) {
        var u = QUser.user;
        var pred = new BooleanBuilder();
        if (s != null && !s.isBlank())
            pred.and(u.username.containsIgnoreCase(s)
                    .or(u.firstName.containsIgnoreCase(s))
                    .or(u.lastName.containsIgnoreCase(s))
                    .or(u.email.containsIgnoreCase(s)));
        return userRepository.findAll(pred, pageable);
    }

    @RequireAuth
    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequireAuth
    @PatchMapping("/me")
    public ResponseEntity<User> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest req) {
        var user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(applyUpdate(user, req));
    }

    @AdminOnly
    @GetMapping("/{id}")
    public ResponseEntity<User> show(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @AdminOnly
    @PatchMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody UpdateProfileRequest req) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(applyUpdate(user, req));
    }

    @AdminOnly
    @GetMapping("/{id}/rentals")
    public Page<ItemRental> rentals(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String s) {
        return itemRentalRepository.findAll(ItemRentalController.rentalPredicate(id, s), pageable);
    }

    record SuspendRequest(String reason) {}

    @AdminOnly
    @PostMapping("/{id}/suspend")
    public ResponseEntity<User> suspend(@PathVariable Long id, @RequestBody(required = false) SuspendRequest req) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setRole(Role.SUSPENDED);
        user.setSuspensionReason(req != null ? req.reason() : null);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @AdminOnly
    @DeleteMapping("/{id}/suspend")
    public ResponseEntity<User> unsuspend(@PathVariable Long id) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setRole(Role.CUSTOMER);
        user.setSuspensionReason(null);
        return ResponseEntity.ok(userRepository.save(user));
    }

    private User applyUpdate(User user, UpdateProfileRequest req) {
        if (req.firstName()   != null) user.setFirstName(req.firstName());
        if (req.lastName()    != null) user.setLastName(req.lastName());
        if (req.email()       != null) user.setEmail(req.email());
        if (req.phoneNumber() != null) user.setPhoneNumber(req.phoneNumber());
        if (req.address()     != null) user.setAddress(req.address());
        return userRepository.save(user);
    }
}
