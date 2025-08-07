package br.com.admissao.service;

import br.com.admissao.dto.ViaCepDTO;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ViaCepClient {

    private final WebClient webClient;

    public ViaCepClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // Retorna Mono e trata erros convertendo para sinal de empty ou erro customizado
    @Retry(name = "viacepRetry")
    public Mono<ViaCepDTO> buscarPorCep(String cep) {
        String normalized = cep.replaceAll("\\D", "");
        return webClient
                .get()
                .uri("/ws/{cep}/json/", normalized)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> Mono.error(new RuntimeException("Erro ao consultar ViaCEP: " + resp.statusCode())))
                .bodyToMono(ViaCepDTO.class)
                .flatMap(v -> {
                    // ViaCEP devolve um JSON com { "erro": true } quando não encontra
                    if (v == null || (v.getCep() == null && v.getLocalidade() == null)) {
                        return Mono.empty();
                    }
                    return Mono.just(v);
                })
                .timeout(java.time.Duration.ofSeconds(2)) // timeout defensivo
                ;
    }

    // opcional: fallback síncrono para ser usado pelo serviço imperativo
    public Mono<ViaCepDTO> buscarPorCepComFallback(String cep, ViaCepDTO fallback) {
        return buscarPorCep(cep).onErrorResume(e -> Mono.justOrEmpty(fallback));
    }
}


