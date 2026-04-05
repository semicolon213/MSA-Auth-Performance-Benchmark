package com.msa.benchmark.gateway.web;

import com.msa.benchmark.common.config.AppProperties;
import com.msa.benchmark.gateway.client.OrderServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayChainController {

    private final OrderServiceClient orderServiceClient;
    private final AppProperties appProperties;

    public GatewayChainController(OrderServiceClient orderServiceClient, AppProperties appProperties) {
        this.orderServiceClient = orderServiceClient;
        this.appProperties = appProperties;
    }

    /**
     * 부하 테스트·엔드투엔드 체인 진입점 (Gateway → Order → Inventory → User).
     */
    @GetMapping("/chain")
    public ResponseEntity<Map<String, Object>> chain() throws InterruptedException {
        int ms = appProperties.getArtificialLatencyMs();
        if (ms > 0) {
            Thread.sleep(ms);
        }
        Map<String, Object> downstream = orderServiceClient.chain();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "gateway");
        body.put("depth", 0);
        body.put("mode", appProperties.getMode());
        body.put("downstream", downstream);
        return ResponseEntity.ok(body);
    }
}
