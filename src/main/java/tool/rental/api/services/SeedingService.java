package tool.rental.api.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import net.datafaker.Faker;
import org.hibernate.SessionFactory;
import org.hibernate.relational.SchemaManager;
import org.springframework.stereotype.Service;
import tool.rental.api.entities.Address;
import tool.rental.api.entities.Item;
import tool.rental.api.entities.ItemRental;
import tool.rental.api.entities.ItemRentalUpdate;
import tool.rental.api.entities.RentalStatus;
import tool.rental.api.entities.Role;
import tool.rental.api.entities.User;
import tool.rental.api.repositories.ItemRentalRepository;
import tool.rental.api.repositories.ItemRentalUpdateRepository;
import tool.rental.api.repositories.ItemRepository;
import tool.rental.api.repositories.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeedingService {

    public record SeedConfig(int customers, int suspended, int items, boolean seedRentals, int rentalsPerCustomer, int cancelledPct, int lostPct) {
        public static SeedConfig defaults() {
            return new SeedConfig(3, 1, 300, true, 80, 20, 5);
        }

        public SeedConfig clamped() {
            return new SeedConfig(
                    Math.clamp(customers, 1, 500),
                    Math.clamp(suspended, 0, 500),
                    Math.clamp(items, 1, 5000),
                    seedRentals,
                    Math.clamp(rentalsPerCustomer, 0, 1000),
                    Math.clamp(cancelledPct, 0, 100),
                    Math.clamp(lostPct, 0, 100)
            );
        }
    }

    private static final int HISTORY_DAYS = 365;
    private static final long SEED = 67L;

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemRentalRepository itemRentalRepository;
    private final ItemRentalUpdateRepository itemRentalUpdateRepository;

    @PersistenceContext
    private EntityManager em;

    public SeedingService(UserRepository userRepository,
                          ItemRepository itemRepository,
                          ItemRentalRepository itemRentalRepository,
                          ItemRentalUpdateRepository itemRentalUpdateRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.itemRentalRepository = itemRentalRepository;
        this.itemRentalUpdateRepository = itemRentalUpdateRepository;
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

    public boolean isDatabaseEmpty() {
        return userRepository.count() == 0;
    }

    @Transactional
    public void seedAll() {
        seedAll(SeedConfig.defaults());
    }

    @Transactional
    public void seedAll(SeedConfig rawCfg) {
        SeedConfig cfg = rawCfg.clamped();
        Random rng = new Random(SEED);
        Faker faker = new Faker(new Random(SEED));
        usedUsernames.clear();
        usedUsernames.add("admin");
        User admin = seedAdmin();
        seedCustomers(faker, cfg.customers());
        seedSuspended(faker, cfg.suspended());
        seedItems(rng, cfg.items());
        if (cfg.seedRentals()) {
            seedRentals(new Random(SEED ^ 0x5eedL), admin, cfg.rentalsPerCustomer(), cfg.cancelledPct(), cfg.lostPct());
        }
    }

    public void wipeAndReseed() {
        wipeAndReseed(SeedConfig.defaults());
    }

    public void wipeAndReseed(SeedConfig cfg) {
        em.clear();
        SessionFactory sf = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        SchemaManager schemaManager = sf.getSchemaManager();
        schemaManager.dropMappedObjects(true);
        schemaManager.exportMappedObjects(true);
        seedAll(cfg);
    }

    private User seedAdmin() {
        User u = new User();
        u.setUsername("admin");
        u.setPassword("admin");
        u.setRole(Role.ADMIN);
        u.setFirstName("Admin");
        u.setLastName("Admin");
        return userRepository.save(u);
    }

    private final Set<String> usedUsernames = new HashSet<>();

    private void seedCustomers(Faker faker, int n) {
        List<User> batch = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            User u = new User();
            u.setPassword("password");
            u.setRole(Role.CUSTOMER);
            populateFakeIdentity(u, faker);
            batch.add(u);
        }
        userRepository.saveAll(batch);
    }

    private void seedSuspended(Faker faker, int n) {
        List<User> batch = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            User u = new User();
            u.setPassword("password");
            u.setRole(Role.SUSPENDED);
            u.setSuspensionReason("Sample suspended account.");
            populateFakeIdentity(u, faker);
            batch.add(u);
        }
        userRepository.saveAll(batch);
    }

    private void populateFakeIdentity(User u, Faker faker) {
        String first = faker.name().firstName();
        String last = faker.name().lastName();
        String username = uniqueUsername(first, last);
        u.setUsername(username);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(username + "@" + faker.internet().domainName());
        u.setPhoneNumber(faker.phoneNumber().cellPhone());

        Address addr = new Address();
        addr.setCountry(faker.address().country());
        addr.setCity(faker.address().city());
        addr.setStreetName(faker.address().streetName());
        addr.setStreetNumber(faker.address().buildingNumber());
        addr.setPostalCode(faker.address().zipCode());
        u.setAddress(addr);
    }

    private String uniqueUsername(String first, String last) {
        String base = slug(first) + "." + slug(last);
        String candidate = base;
        int suffix = 2;
        while (!usedUsernames.add(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private static String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private void seedItems(Random rng, int count) {
        List<Item> items = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
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

    private record LifecycleEvent(RentalStatus status, Instant at, User actor) {}

    private void seedRentals(Random rng, User admin, int rentalsPerCustomer, int cancelledPct, int lostPct) {
        List<User> customers = userRepository.findAllByRole(Role.CUSTOMER);
        List<Item> items = itemRepository.findAll();
        if (customers.isEmpty() || items.isEmpty() || rentalsPerCustomer == 0) return;

        LocalDate today = LocalDate.now();
        Instant nowInstant = Instant.now();
        ZoneId zone = ZoneId.systemDefault();

        int total = customers.size() * rentalsPerCustomer;
        List<ItemRental> rentals = new ArrayList<>(total);
        List<List<LifecycleEvent>> eventsPerRental = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            User user = customers.get(i % customers.size());
            Item item = items.get(rng.nextInt(items.size()));

            int daysAgo = rng.nextInt(HISTORY_DAYS);
            LocalDate startDate = today.minusDays(daysAgo);
            LocalDate dueDate = startDate.plusDays(item.getRentPeriod());

            List<LifecycleEvent> events = simulateLifecycle(startDate, dueDate, today, nowInstant, zone, user, admin, rng, cancelledPct, lostPct);

            ItemRental rental = new ItemRental();
            rental.setItem(item);
            rental.setUser(user);
            rental.setStartDate(startDate);
            rental.setDueDate(dueDate);
            rental.setAmount(rng.nextInt(3) + 1);
            rental.setStatus(events.get(events.size() - 1).status());

            rentals.add(rental);
            eventsPerRental.add(events);
        }

        itemRentalRepository.saveAll(rentals);

        List<ItemRentalUpdate> updates = new ArrayList<>(total * 3);
        for (int i = 0; i < rentals.size(); i++) {
            ItemRental saved = rentals.get(i);
            for (LifecycleEvent ev : eventsPerRental.get(i)) {
                ItemRentalUpdate upd = new ItemRentalUpdate();
                upd.setRental(saved);
                upd.setStatus(ev.status());
                upd.setCreatedBy(ev.actor());
                upd.setCreatedAt(ev.at());
                updates.add(upd);
            }
        }
        itemRentalUpdateRepository.saveAll(updates);
    }

    /**
     * Lifecycle rules:
     *   PENDING -> SENT -> DELIVERED -> RETURNED | LOST
     *   PENDING -> CANCELLED   (cancellation only valid before SENT)
     */
    private List<LifecycleEvent> simulateLifecycle(
            LocalDate startDate, LocalDate dueDate, LocalDate today, Instant now,
            ZoneId zone, User customer, User admin, Random rng, int cancelledPct, int lostPct
    ) {
        List<LifecycleEvent> events = new ArrayList<>();

        Instant createdAt = startDate.minusDays(1 + rng.nextInt(3))
                .atTime(LocalTime.of(8 + rng.nextInt(12), rng.nextInt(60)))
                .atZone(zone).toInstant();
        events.add(new LifecycleEvent(RentalStatus.PENDING, createdAt, customer));

        boolean isFuture = startDate.isAfter(today);
        boolean isPast = dueDate.isBefore(today);

        if (isFuture) return events;

        // Cancel from PENDING - the ONLY valid path to CANCELLED.
        if (rng.nextInt(100) < cancelledPct) {
            Instant cancelAt = createdAt.plus(1 + rng.nextInt(20), ChronoUnit.HOURS);
            if (cancelAt.isAfter(now)) cancelAt = now.minus(1, ChronoUnit.MINUTES);
            events.add(new LifecycleEvent(RentalStatus.CANCELLED, cancelAt, customer));
            return events;
        }

        Instant sentAt = startDate.atTime(LocalTime.of(9 + rng.nextInt(6), rng.nextInt(60)))
                .atZone(zone).toInstant();
        if (sentAt.isAfter(now)) return events;
        events.add(new LifecycleEvent(RentalStatus.SENT, sentAt, admin));

        Instant deliveredAt = sentAt.plus(1 + rng.nextInt(2), ChronoUnit.DAYS)
                .plus(rng.nextInt(8), ChronoUnit.HOURS);
        if (deliveredAt.isAfter(now)) return events;
        events.add(new LifecycleEvent(RentalStatus.DELIVERED, deliveredAt, admin));

        if (!isPast) return events;

        // After DELIVERED only RETURNED or LOST are valid terminals.
        Instant terminalAt = dueDate.atTime(LocalTime.of(10 + rng.nextInt(8), rng.nextInt(60)))
                .atZone(zone).toInstant().plus(rng.nextInt(48) - 24, ChronoUnit.HOURS);
        if (terminalAt.isAfter(now)) terminalAt = now.minus(1, ChronoUnit.MINUTES);
        if (terminalAt.isBefore(deliveredAt)) terminalAt = deliveredAt.plus(1, ChronoUnit.HOURS);

        boolean lost = rng.nextInt(100) < lostPct;
        RentalStatus terminal = lost ? RentalStatus.LOST : RentalStatus.RETURNED;
        User terminalActor = lost ? admin : customer;
        events.add(new LifecycleEvent(terminal, terminalAt, terminalActor));
        return events;
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
