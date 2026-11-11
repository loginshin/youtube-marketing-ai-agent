package com.marketing.agent.usage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/usage")
public class ApiUsageController {
    private final ApiUsageRepository repository;

    public ApiUsageController(ApiUsageRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ApiUsageRepository.Summary> summary(@RequestParam(defaultValue = "7") int days) {
        if (days != 1 && days != 7 && days != 30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return repository.summarize(LocalDate.now(zone).minusDays(days - 1L).atStartOfDay(zone).toInstant());
    }
}
