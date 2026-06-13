package tool.rental.api.services;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tool.rental.api.entities.ItemRental;
import tool.rental.api.entities.Role;
import tool.rental.api.entities.User;

import java.util.List;

public record AppUserDetails(User user) implements UserDetails {

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getUsername(); }

    public boolean canAccess(ItemRental rental) {
        return user.getRole() == Role.ADMIN || rental.getUser().getId().equals(user.getId());
    }
}
