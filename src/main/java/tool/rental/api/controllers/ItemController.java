package tool.rental.api.controllers;

import com.querydsl.core.BooleanBuilder;
import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.entities.Item;
import tool.rental.api.entities.QItem;
import tool.rental.api.repositories.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public Page<Item> index(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String s,
            @RequestParam(required = false) Boolean available) {
        var item = QItem.item;
        var pred = new BooleanBuilder();
        if (s != null && !s.isBlank())
            pred.and(item.name.containsIgnoreCase(s)
                    .or(item.producer.containsIgnoreCase(s))
                    .or(item.description.containsIgnoreCase(s)));
        if (available != null && available)
            pred.and(item.availableAmount.gt(0));
        return itemRepository.findAll(pred, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> show(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @AdminOnly
    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Item item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemRepository.save(item));
    }

    @AdminOnly
    @PatchMapping("/{id}")
    public ResponseEntity<Item> update(@PathVariable Long id, @RequestBody Item item) {
        if (!itemRepository.existsById(id)) return ResponseEntity.notFound().build();

        item.setId(id);

        return ResponseEntity.ok(itemRepository.save(item));
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var item = itemRepository.findById(id).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        itemRepository.delete(item);
        return ResponseEntity.noContent().build();
    }
}
