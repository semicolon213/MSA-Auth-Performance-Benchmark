package com.msa.benchmark.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "jwt-relay")
public class FeignAuthorizationRelayInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        String auth = attrs.getRequest().getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            template.header("Authorization", auth);
        }
    }
}
