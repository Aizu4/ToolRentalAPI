package tool.rental.api.controllers;

import tool.rental.api.entities.Address;
import tool.rental.api.entities.Role;
import tool.rental.api.entities.User;
import tool.rental.api.repositories.UserRepository;
import tool.rental.api.services.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository, JwtService jwtService,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public record AuthRequest(String username, String password) {}
    public record AuthResponse(String token) {}

    record RegisterRequest(
            String username, String password,
            String firstName, String lastName,
            String email, String phoneNumber,
            Address address
    ) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent())
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        var user = new User();
        user.setUsername(req.username());
        user.setPassword(req.password());
        user.setRole(Role.CUSTOMER);
        if (req.firstName()   != null) user.setFirstName(req.firstName());
        if (req.lastName()    != null) user.setLastName(req.lastName());
        if (req.email()       != null) user.setEmail(req.email());
        if (req.phoneNumber() != null) user.setPhoneNumber(req.phoneNumber());
        if (req.address()     != null) user.setAddress(req.address());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(jwtService.generateToken(req.username(), Role.CUSTOMER.name())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            var role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElseThrow();
            return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(req.username(), role)));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
