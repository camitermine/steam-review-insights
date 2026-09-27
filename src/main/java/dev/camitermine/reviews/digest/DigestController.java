package dev.camitermine.reviews.digest;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.camitermine.reviews.digest.DigestService.Digest;
import dev.camitermine.reviews.steam.SteamProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/reviews")
@Validated
@Tag(name = "Digest", description = "Summary used by the weekly n8n report")
public class DigestController {

    private final DigestService digestService;
    private final SteamProperties steamProperties;

    public DigestController(DigestService digestService, SteamProperties steamProperties) {
        this.digestService = digestService;
        this.steamProperties = steamProperties;
    }

    @Operation(summary = "Summary of the last N days",
            description = "Counts per category plus the newest bug reports and feature requests.")
    @GetMapping("/digest")
    public Digest digest(@RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        return digestService.lastDays(steamProperties.appId(), days);
    }
}
