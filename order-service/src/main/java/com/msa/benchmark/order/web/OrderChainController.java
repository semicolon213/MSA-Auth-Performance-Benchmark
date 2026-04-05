package com.msa.benchmark.order.web;

import com.msa.benchmark.common.config.AppProperties;
import com.msa.benchmark.order.client.InventoryServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class OrderChainController {

    private final InventoryServiceClient inventoryServiceClient;
    private final AppProperties appProperties;

    public OrderChainController(InventoryServiceClient inventoryServiceClient, AppProperties appProperties) {
        this.inventoryServiceClient = inventoryServiceClient;
        this.appProperties = appProperties;
    }

    @GetMapping("/chain")
    public ResponseEntity<Map<String, Object>> chain() throws InterruptedException {
        int ms = appProperties.getArtificialLatencyMs();
        if (ms > 0) {
            Thread.sleep(ms);
        }
        Map<String, Object> downstream = inventoryServiceClient.chain();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "order");
        body.put("depth", 1);
        body.put("mode", appProperties.getMode());
        body.put("downstream", downstream);
        return ResponseEntity.ok(body);
    }
}
