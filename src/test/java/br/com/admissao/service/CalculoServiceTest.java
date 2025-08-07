package br.com.admissao.service;

import br.com.admissao.dto.CalculoRequestDTO;
import br.com.admissao.dto.CalculoResponseDTO;
import br.com.admissao.dto.ViaCepDTO;
import br.com.admissao.exception.ApiException;
import br.com.admissao.model.Admissao;
import br.com.admissao.repository.AdmissaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CalculoServiceTest {

    @Mock
    private AdmissaoRepository repository;

    @Mock
    private ViaCepClient viaCepClient;

    @InjectMocks
    private CalculoService service;

    private CalculoRequestDTO request;

    @BeforeEach
    void setUp() {
        request = CalculoRequestDTO.builder()
                .dataAdmissao(LocalDate.of(2022, 5, 10))
                .salarioBruto(BigDecimal.valueOf(3500))
                .cep("66050080")
                .build();
    }

    @Test
    void calcularESalvarReactive_success() {
        // arrange
        Admissao saved = Admissao.builder()
                .id("abc123")
                .dataAdmissao(request.getDataAdmissao())
                .salarioBruto(request.getSalarioBruto())
                .anos(3)
                .meses(2)
                .dias(26)
                .porcentagem35(request.getSalarioBruto().multiply(BigDecimal.valueOf(0.35)))
                .criadoEm(LocalDateTime.now())
                .build();

        ViaCepDTO viaCepDTO = new ViaCepDTO();
        viaCepDTO.setCep("66050-080");
        viaCepDTO.setLocalidade("Belém");

        when(repository.save(any(Admissao.class))).thenReturn(saved);
        when(viaCepClient.buscarPorCep(anyString())).thenReturn(Mono.just(viaCepDTO));

        // act
        Mono<CalculoResponseDTO> resultMono = service.calcularESalvarReactive(request);

        // assert
        StepVerifier.create(resultMono)
                .assertNext(resp -> {
                    // validações mínimas
                    org.assertj.core.api.Assertions.assertThat(resp).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(resp.getId()).isEqualTo("abc123");
                    org.assertj.core.api.Assertions.assertThat(resp.getEndereco()).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(resp.getEndereco().getLocalidade()).isEqualTo("Belém");
                    org.assertj.core.api.Assertions.assertThat(resp.getPorcentagem35()).isEqualByComparingTo(request.getSalarioBruto().multiply(BigDecimal.valueOf(0.35)));
                })
                .verifyComplete();

        // verify interactions
        verify(repository, times(1)).save(any(Admissao.class));
        verify(viaCepClient, times(1)).buscarPorCep("66050080");
    }

    @Test
    void calcularESalvarReactive_viaCepEmpty_shouldEmitApiException() {
        // arrange: repository returns saved entity, but viaCep returns empty
        Admissao saved = Admissao.builder()
                .id("id-empty")
                .dataAdmissao(request.getDataAdmissao())
                .salarioBruto(request.getSalarioBruto())
                .criadoEm(LocalDateTime.now())
                .build();

        when(repository.save(any(Admissao.class))).thenReturn(saved);
        when(viaCepClient.buscarPorCep(anyString())).thenReturn(Mono.empty());

        // act
        Mono<CalculoResponseDTO> resultMono = service.calcularESalvarReactive(request);

        // assert: espera ApiException (switchIfEmpty -> Mono.error(ApiException))
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ApiException
                                && throwable.getMessage().contains("ViaCEP não retornou dados"))
                .verify();

        verify(repository, times(1)).save(any(Admissao.class));
        verify(viaCepClient, times(1)).buscarPorCep("66050080");
    }
}
