package tool.rental.api.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tool.rental.api.entities.ItemRentalUpdate;

import java.util.List;

public interface ItemRentalUpdateRepository extends JpaRepository<ItemRentalUpdate, Long> {
    @EntityGraph(attributePaths = {"createdBy"})
    List<ItemRentalUpdate> findByRentalIdOrderByCreatedAtAsc(Long rentalId);
}
