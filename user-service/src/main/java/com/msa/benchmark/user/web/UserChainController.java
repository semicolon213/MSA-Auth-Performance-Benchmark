package com.msa.benchmark.user.web;

import com.msa.benchmark.common.config.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class UserChainController {

    private final AppProperties appProperties;

    public UserChainController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/chain")
    public ResponseEntity<Map<String, Object>> chain() throws InterruptedException {
        int ms = appProperties.getArtificialLatencyMs();
        if (ms > 0) {
            Thread.sleep(ms);
        }
        return ResponseEntity.ok(Map.of(
                "service", "user",
                "depth", 3,
                "mode", appProperties.getMode()
        ));
    }
}
