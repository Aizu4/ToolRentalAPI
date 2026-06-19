package tool.rental.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter private Long id;

    @Column(nullable = false, unique = true)
    @Setter private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter private Role role;

    @Setter private String firstName;
    @Setter private String lastName;

    @Column(unique = true)
    @Setter private String email;

    @Setter private String phoneNumber;

    @Setter private String suspensionReason;

    @Embedded
    @Setter private Address address;

    public void setPassword(String rawPassword) {
        this.password = PASSWORD_ENCODER.encode(rawPassword);
    }
}
