package tool.rental.api;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tool.rental.api.services.SeedingService;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private final SeedingService seedingService;

    public DatabaseSeeder(SeedingService seedingService) {
        this.seedingService = seedingService;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!seedingService.isDatabaseEmpty()) return;
        seedingService.seedAll();
    }
}
