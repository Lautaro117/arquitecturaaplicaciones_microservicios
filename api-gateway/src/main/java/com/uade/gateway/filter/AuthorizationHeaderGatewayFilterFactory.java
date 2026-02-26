package com.uade.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * GatewayFilter que propaga explícitamente el header Authorization a los servicios downstream.
 * Garantiza que el JWT validado por el Gateway llegue a los microservicios protegidos.
 */
@Component
public class AuthorizationHeaderGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthorizationHeaderGatewayFilterFactory.Config> {

    private static final String AUTHORIZATION = HttpHeaders.AUTHORIZATION;

    public AuthorizationHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            Optional<String> authHeader = request.getHeaders().getOrEmpty(AUTHORIZATION).stream().findFirst();

            if (authHeader.isPresent()) {
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header(AUTHORIZATION, authHeader.get())
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Sin configuración adicional necesaria
    }
}
