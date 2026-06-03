package tool.rental.api.controllers;

import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.entities.Item;
import tool.rental.api.repositories.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public Page<Item> index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String s,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        Sort sorting = (sort != null && !sort.isBlank())
                ? Sort.by("desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sort)
                : Sort.unsorted();
        var pageable = PageRequest.of(page, pageSize, sorting);
        boolean hasSearch = s != null && !s.isBlank();
        boolean onlyAvailable = available != null && available;
        if (hasSearch && onlyAvailable) return itemRepository.searchAvailable(s, pageable);
        if (hasSearch)                  return itemRepository.search(s, pageable);
        if (onlyAvailable)              return itemRepository.findAvailable(pageable);
        return itemRepository.findAll(pageable);
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

        return ResponseEntity.status(HttpStatus.CREATED).body(itemRepository.save(item));
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) return ResponseEntity.notFound().build();

        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
