package com.msa.benchmark.inventory.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(
        name = "user-service",
        url = "${services.user.url}"
)
public interface UserServiceClient {

    @GetMapping("/api/internal/chain")
    Map<String, Object> chain();
}
