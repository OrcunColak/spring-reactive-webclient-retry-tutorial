# Read me

The original idea is from  
https://blog.stackademic.com/spring-boot-retry-mechanism-dcc56ecbe358

The example uses retryWhen

```
public Mono<String> fetchData() {
  return webClient.get()
    .uri("/data")
    .retrieve()
    .bodyToMono(String.class)
    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
      .filter(throwable -> throwable instanceof WebClientResponseException)
      .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
        new RuntimeException("Retries exhausted: " + retrySignal.failure())))
       .onErrorResume(e -> {
         // Handle error after retries are exhausted
         return Mono.just("Fallback data");
    });
  }
``` 

method
