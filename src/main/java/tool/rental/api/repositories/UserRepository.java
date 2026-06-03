package tool.rental.api.repositories;

import tool.rental.api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:s,'%'))")
    Page<User> search(@Param("s") String s, Pageable pageable);
}
