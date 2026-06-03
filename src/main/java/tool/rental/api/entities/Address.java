package tool.rental.api.entities;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Address {
    private String country;
    private String city;
    private String streetName;
    private String streetNumber;
    private String apartment;
    private String postalCode;
}
