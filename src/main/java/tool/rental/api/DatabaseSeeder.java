package tool.rental.api;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tool.rental.api.entities.Item;
import tool.rental.api.entities.Role;
import tool.rental.api.entities.User;
import tool.rental.api.repositories.ItemRepository;
import tool.rental.api.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public DatabaseSeeder(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    private static final List<String> PRODUCERS = List.of(
            "DeWalt", "Makita", "Bosch", "Milwaukee", "Black & Decker",
            "Ryobi", "Hilti", "Stanley", "Metabo", "Festool",
            "Ridgid", "Skil", "Porter-Cable", "Hitachi", "Werner",
            "Louisville Ladder", "Husqvarna", "Stihl", "Snap-on", "Klein Tools"
    );

    private static final List<String> ITEM_TYPES = List.of(
            "Power Drill", "Circular Saw", "Orbital Sander", "Jigsaw", "Reciprocating Saw",
            "Angle Grinder", "Belt Sander", "Random Orbital Sander", "Detail Sander",
            "Impact Driver", "Hammer Drill", "Rotary Hammer", "Demolition Hammer", "Router",
            "Planer", "Jointer", "Band Saw", "Miter Saw", "Table Saw", "Scroll Saw",
            "Chain Saw", "Hedge Trimmer", "String Trimmer", "Leaf Blower", "Pressure Washer",
            "Air Compressor", "Nail Gun", "Staple Gun", "Heat Gun", "Extension Ladder",
            "Step Ladder", "Scaffold", "Drywall Sander", "Tile Saw", "Tile Cutter",
            "Concrete Mixer", "Plate Compactor", "Rammer", "Jackhammer", "Core Drill",
            "Floor Grinder", "Floor Sander", "Paint Sprayer", "Airless Sprayer",
            "Caulking Gun", "Electric Screwdriver", "Multi-Tool", "Oscillating Tool",
            "Die Grinder", "Pipe Threader"
    );

    private static final int[] RENT_PERIODS = {3, 7, 14, 21, 28};

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!isDatabaseEmpty()) return;

        seedUsers();
        seedItems();
    }

    private boolean isDatabaseEmpty() {
        return (userRepository.count() == 0);
    }

    private void seedUsers() {
        record Seed(String username, String password, Role role, String firstName, String lastName) {
        }

        List.of(
                new Seed("admin", "admin", Role.ADMIN, "Alice", "Admin"),
                new Seed("user1", "password", Role.CUSTOMER, "Bob", "Builder"),
                new Seed("user2", "password", Role.CUSTOMER, "Carol", "Carpenter"),
                new Seed("user3", "password", Role.CUSTOMER, "Dave", "Digger"),
                new Seed("suspended1", "password", Role.SUSPENDED, "Eve", "Example")
        ).forEach(s -> {
            User u = new User();
            u.setUsername(s.username());
            u.setPassword(s.password());
            u.setRole(s.role());
            u.setFirstName(s.firstName());
            u.setLastName(s.lastName());
            userRepository.save(u);
        });
    }

    private void seedItems() {
        Random rng = new Random(67);
        List<Item> items = new ArrayList<>(300);

        for (int i = 0; i < 300; i++) {
            String manufacturer = PRODUCERS.get(rng.nextInt(PRODUCERS.size()));
            String name = ITEM_TYPES.get(rng.nextInt(ITEM_TYPES.size()));
            String model = generateModel(manufacturer, name, rng);

            int totalAmount = (rng.nextInt(9) + 1) * 5;

            Item item = new Item();
            item.setProducer(manufacturer);
            item.setName(name);
            item.setModel(model);
            item.setDescription(name + " manufactured by " + manufacturer + ", model " + model + ".");
            item.setTotalAmount(totalAmount);
            item.setRentPeriod(RENT_PERIODS[Math.abs(name.hashCode()) % RENT_PERIODS.length]);
            items.add(item);
        }

        itemRepository.saveAll(items);
    }

    private String generateModel(String producer, String itemType, Random rng) {
        int h = Math.abs(producer.hashCode());
        int numCount = h % 3 + 2;
        int letterCount = h % 3;

        String prefix = initials(producer, "[\\s&-]+") + initials(itemType, "\\s+");
        String nums = String.format("%0" + numCount + "d", rng.nextInt((int) Math.pow(10, numCount)));
        String allLetters = producer.toUpperCase().replaceAll("[^A-Z]", "");

        return prefix + "-" + nums + allLetters.substring(0, Math.min(letterCount, allLetters.length()));
    }

    private static String initials(String text, String delimPattern) {
        return Arrays.stream(text.split(delimPattern))
                .filter(w -> !w.isEmpty() && Character.isLetter(w.charAt(0)))
                .map(w -> String.valueOf(Character.toUpperCase(w.charAt(0))))
                .collect(Collectors.joining());
    }
}
