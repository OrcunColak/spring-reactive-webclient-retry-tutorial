package com.colak.springtutorial.quote.external.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/quote")
public class QuoteController {

    private AtomicInteger counter = new AtomicInteger();

    // http://localhost:8080/api/v1/quote/getQuotes
    @GetMapping(path = "getQuotes")
    public Flux<String> getQuotes() {
        if (counter.getAndIncrement() < 2) {
            throw new RuntimeException("Counter error");
        }
        List<String> list = List.of("quote1", "quote2");
        return Flux.fromIterable(list);
    }
}
