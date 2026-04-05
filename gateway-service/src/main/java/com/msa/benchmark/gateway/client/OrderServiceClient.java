package com.msa.benchmark.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(
        name = "order-service",
        url = "${services.order.url}"
)
public interface OrderServiceClient {

    @GetMapping("/api/internal/chain")
    Map<String, Object> chain();
}
