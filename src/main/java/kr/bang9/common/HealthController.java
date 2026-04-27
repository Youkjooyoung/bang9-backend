package kr.bang9.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "status", "ok",
            "service", "bang9-backend",
            "timestamp", OffsetDateTime.now()
        );
    }
}
