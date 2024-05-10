package com.colak.springreactivewebclienttutorial.quote.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
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
                .retryWhen(getRetry());

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
                .filter(throwable ->
                        throwable instanceof WebClientResponseException webClientResponseException
                        && webClientResponseException.getStatusCode().is5xxServerError()
                        || throwable.getCause() instanceof TimeoutException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.info("SERVICE_UNAVAILABLE | External Service failed to process after max retries");
                    throw new RuntimeException("SERVICE_UNAVAILABLE");
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
