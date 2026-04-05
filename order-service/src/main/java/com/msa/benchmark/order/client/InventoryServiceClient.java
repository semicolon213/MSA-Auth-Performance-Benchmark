package com.msa.benchmark.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(
        name = "inventory-service",
        url = "${services.inventory.url}"
)
public interface InventoryServiceClient {

    @GetMapping("/api/internal/chain")
    Map<String, Object> chain();
}
