package tool.rental.api.repositories;

import tool.rental.api.entities.ItemRental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRentalRepository extends JpaRepository<ItemRental, Long> {

    @Query("SELECT r FROM ItemRental r WHERE LOWER(r.item.name) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(r.item.producer) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(r.item.description) LIKE LOWER(CONCAT('%',:s,'%'))")
    Page<ItemRental> search(@Param("s") String s, Pageable pageable);

    @Query("SELECT r FROM ItemRental r WHERE r.user.id = :userId")
    Page<ItemRental> findByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM ItemRental r WHERE r.user.id = :userId AND (LOWER(r.item.name) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(r.item.producer) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(r.item.description) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<ItemRental> searchByUser(@Param("userId") Long userId, @Param("s") String s, Pageable pageable);
}
