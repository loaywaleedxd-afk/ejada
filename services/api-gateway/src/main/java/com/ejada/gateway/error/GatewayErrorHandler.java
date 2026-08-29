package com.ejada.gateway.error;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayErrorHandler extends ResponseStatusExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String message;
        Throwable cause = ex instanceof WebClientRequestException && ex.getCause() != null ? ex.getCause() : ex;

        if (cause instanceof TimeoutException || cause instanceof ReadTimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "The service is currently down, please wait a moment and try again.";
        } else if (cause instanceof ConnectException || cause instanceof ConnectTimeoutException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "The service is currently down, please wait a moment and try again.";
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = status == HttpStatus.NOT_FOUND ? "No route matches this path." : status.getReasonPhrase();
        } else {
            status = HttpStatus.BAD_GATEWAY;
            message = "The service is currently down, please wait a moment and try again.";
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
