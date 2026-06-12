package tool.rental.api.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.services.SeedingService;
import tool.rental.api.services.SeedingService.SeedConfig;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@AdminOnly
public class AdminController {

    private final SeedingService seedingService;

    public AdminController(SeedingService seedingService) {
        this.seedingService = seedingService;
    }

    @PostMapping("/db/reset")
    public Map<String, String> resetDatabase(@RequestBody(required = false) SeedConfig config) {
        seedingService.wipeAndReseed(config == null ? SeedConfig.defaults() : config);
        return Map.of("status", "ok", "message", "Database reset and reseeded with sample data.");
    }
}
