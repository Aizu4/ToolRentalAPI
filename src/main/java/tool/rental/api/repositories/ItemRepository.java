package tool.rental.api.repositories;

import tool.rental.api.entities.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(i.producer) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%',:s,'%'))")
    Page<Item> search(@Param("s") String s, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE (i.totalAmount - COALESCE((SELECT SUM(ir.amount) FROM ItemRental ir WHERE ir.item = i), 0)) > 0")
    Page<Item> findAvailable(Pageable pageable);

    @Query("SELECT i FROM Item i WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(i.producer) LIKE LOWER(CONCAT('%',:s,'%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%',:s,'%'))) AND (i.totalAmount - COALESCE((SELECT SUM(ir.amount) FROM ItemRental ir WHERE ir.item = i), 0)) > 0")
    Page<Item> searchAvailable(@Param("s") String s, Pageable pageable);
}
