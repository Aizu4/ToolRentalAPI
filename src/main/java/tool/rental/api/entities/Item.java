package tool.rental.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Getter
@Setter
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private String producer;

    @Column(nullable = false)
    private String model;

    @Column
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer rentPeriod; // days

    @Formula("total_amount - COALESCE((SELECT SUM(ir.amount) FROM item_rental ir WHERE ir.item_id = id AND ir.status NOT IN ('RETURNED', 'CANCELLED', 'LOST')), 0)")
    private Integer availableAmount;
}
