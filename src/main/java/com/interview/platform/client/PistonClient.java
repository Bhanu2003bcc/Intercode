package com.interview.platform.client;

import com.interview.platform.dto.piston.PistonRequestDto;
import com.interview.platform.dto.piston.PistonResponseDto;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class PistonClient {

    private static final Logger log = LoggerFactory.getLogger(PistonClient.class);

    private final WebClient webClient;
    private final int maxRetries;

    public PistonClient(
            @Value("${app.piston.base-url}") String baseUrl,
            @Value("${app.piston.timeout-seconds}") int timeoutSeconds,
            @Value("${app.piston.max-retries}") int maxRetries) {
        
        this.maxRetries = maxRetries;

        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public PistonResponseDto execute(PistonRequestDto requestDto) {
        log.info("Sending code execution request to Piston for language: {}", requestDto.getLanguage());

        return this.webClient.post()
                .uri("/execute")
                .bodyValue(requestDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Piston API returned error status: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("Piston API error body: {}", body);
                                return Mono.error(new RuntimeException("Piston API error: " + response.statusCode() + " - " + body));
                            });
                })
                .bodyToMono(PistonResponseDto.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            log.warn("Retrying Piston request due to error: {}", throwable.getMessage());
                            return true;
                        }))
                .block();
    }
}
