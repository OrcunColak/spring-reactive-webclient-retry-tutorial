package com.colak.springtutorial.quote.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class QuoteClient {

    public Flux<String> getQuotesByWebFlux() {
        WebClient webClient = WebClient.create("http://localhost:8080/api/v1/quote");

        List<Flux<String>> list = new ArrayList<>();

        Flux<String> flux = webClient
                .get()
                .uri("/getQuotes")
                .retrieve()
                .bodyToFlux(String.class)

                // The retry method is a simple technique in which, in the event that the web client returns an error,
                // the application retries the request a predetermined number of times.
                // Here we retry for 3 Attempts
                // .retry(3);

                // We can make use of the retryWhen method for a more dynamic and flexible retry strategy.
                // We are able to construct a more complex retry logic with this way.
                .retryWhen(getRetry())
                .onErrorResume(exception -> {
                    // Handle error after retries are exhausted
                    return Mono.just("Fallback data");
                });

        list.add(flux);
        return Flux.merge(list);
    }

    private Retry getRetry() {
        return Retry
                // Retry up to 3 times with a backoff of 1 second
                .backoff(3, Duration.ofSeconds(1))
                // Maximum backoff of 5 seconds
                .maxBackoff(Duration.ofSeconds(5))
                .doAfterRetry(retrySignal -> log.info("RETRY_ATTEMPTED"))
                // A retry attempt will now be made in the event of any service problems, including 4xx errors
                .filter(throwable -> {
                            boolean result = false;
                            if (throwable instanceof WebClientResponseException webClientResponseException) {
                                if (webClientResponseException.getStatusCode().is5xxServerError()) {
                                    log.info("is5xxServerError");
                                    result = true;
                                }
                            } else if (throwable.getCause() instanceof TimeoutException) {
                                log.info("TimeoutException");
                                result = true;
                            }
                            return result;
                        }
                )
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.info("SERVICE_UNAVAILABLE | External Service failed to process after max retries");
                    throw new RuntimeException("SERVICE_UNAVAILABLE : " + retrySignal.failure());
                });
    }

    public List<String> getQuotes() {
        Flux<String> flux = getQuotesByWebFlux();

        return flux
                // Merge all flux into a single Mono<List<String>>
                .collectList()
                // Convert Mono<List<String>> to List<String >
                .block();
    }
}
