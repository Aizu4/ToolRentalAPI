package tool.rental.api.repositories;

import com.querydsl.core.types.Predicate;
import tool.rental.api.entities.ItemRental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

public interface ItemRentalRepository extends JpaRepository<ItemRental, Long>, QuerydslPredicateExecutor<ItemRental> {

    @Override
    @EntityGraph(attributePaths = {"item", "user"})
    Page<ItemRental> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"item", "user"})
    Page<ItemRental> findAll(Predicate predicate, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"item", "user"})
    Optional<ItemRental> findById(Long id);
}
