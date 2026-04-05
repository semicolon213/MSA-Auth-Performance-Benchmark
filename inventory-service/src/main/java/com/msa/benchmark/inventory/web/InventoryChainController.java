package com.msa.benchmark.inventory.web;

import com.msa.benchmark.common.config.AppProperties;
import com.msa.benchmark.inventory.client.UserServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class InventoryChainController {

    private final UserServiceClient userServiceClient;
    private final AppProperties appProperties;

    public InventoryChainController(UserServiceClient userServiceClient, AppProperties appProperties) {
        this.userServiceClient = userServiceClient;
        this.appProperties = appProperties;
    }

    @GetMapping("/chain")
    public ResponseEntity<Map<String, Object>> chain() throws InterruptedException {
        int ms = appProperties.getArtificialLatencyMs();
        if (ms > 0) {
            Thread.sleep(ms);
        }
        Map<String, Object> downstream = userServiceClient.chain();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "inventory");
        body.put("depth", 2);
        body.put("mode", appProperties.getMode());
        body.put("downstream", downstream);
        return ResponseEntity.ok(body);
    }
}
