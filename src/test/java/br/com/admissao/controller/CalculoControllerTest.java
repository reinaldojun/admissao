package br.com.admissao.controller;

import br.com.admissao.dto.CalculoRequestDTO;
import br.com.admissao.dto.CalculoResponseDTO;
import br.com.admissao.dto.ViaCepDTO;
import br.com.admissao.model.Admissao;
import br.com.admissao.service.CalculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CalculoControllerTest {

    @Mock
    private CalculoService service;

    @InjectMocks
    private CalculoController controller;

    private CalculoRequestDTO request;
    private CalculoResponseDTO responseDto;
    private Admissao a1; // <-- campos da classe
    private Admissao a2; // <-- campos da classe

    @BeforeEach
    void setUp() {
        request = CalculoRequestDTO.builder()
                .dataAdmissao(LocalDate.of(2022, 5, 10))
                .salarioBruto(BigDecimal.valueOf(3500))
                .cep("66050080")
                .build();

        ViaCepDTO viaCepDTO = new ViaCepDTO();
        viaCepDTO.setCep("66050-080");
        viaCepDTO.setLocalidade("Belém");

        responseDto = CalculoResponseDTO.builder()
                .id("ctrl-1")
                .dataAdmissao(request.getDataAdmissao())
                .salarioBruto(request.getSalarioBruto())
                .anos(3)
                .meses(2)
                .dias(26)
                .porcentagem35(request.getSalarioBruto().multiply(BigDecimal.valueOf(0.35)))
                .criadoEm(LocalDateTime.now())
                .endereco(viaCepDTO)
                .build();

        // Inicializa os objetos a1 e a2 aqui (no @BeforeEach)
        a1 = buildAdmissao("id-1", LocalDate.of(2022, 5, 10), BigDecimal.valueOf(3500));
        a2 = buildAdmissao("id-2", LocalDate.of(2023, 6, 15), BigDecimal.valueOf(4200));
    }

    @Test
    void calcular_shouldReturnCreatedResponse() {
        // arrange
        when(service.calcularESalvarReactive(any(CalculoRequestDTO.class))).thenReturn(Mono.just(responseDto));

        // act
        Mono<ResponseEntity<CalculoResponseDTO>> responseMono = controller.calcular(request);

        // assert
        StepVerifier.create(responseMono)
                .assertNext(responseEntity -> {
                    assertThat(responseEntity.getStatusCodeValue()).isEqualTo(201);
                    assertThat(responseEntity.getBody()).isNotNull();
                    assertThat(responseEntity.getBody().getId()).isEqualTo("ctrl-1");
                })
                .verifyComplete();

        verify(service, times(1)).calcularESalvarReactive(any(CalculoRequestDTO.class));
    }

    @Test
    void calcular_whenServiceErrors_shouldPropagate() {
        // arrange
        when(service.calcularESalvarReactive(any(CalculoRequestDTO.class)))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        // act
        Mono<ResponseEntity<CalculoResponseDTO>> responseMono = controller.calcular(request);

        // assert: espera erro no Mono
        StepVerifier.create(responseMono)
                .expectErrorMessage("boom")
                .verify();

        verify(service, times(1)).calcularESalvarReactive(any(CalculoRequestDTO.class));
    }

    // ---------------- GET ----------------
    @Test
    void listar_shouldReturnPagedResult_withDefaults() {
        // arrange
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by("criadoEm").descending());
        Page<Admissao> page = new PageImpl<>(List.of(a1, a2), expectedPageable, 2);

        when(service.listar(argThat(p ->
                p.getPageNumber() == 0 &&
                        p.getPageSize() == 20 &&
                        p.getSort().equals(Sort.by("criadoEm").descending())
        ))).thenReturn(page);

        // act
        ResponseEntity<Page<Admissao>> resp = controller.listar(0, 20, null);

        // assert
        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        Page<Admissao> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTotalElements()).isEqualTo(2);
        assertThat(body.getContent()).containsExactly(a1, a2);

        verify(service, times(1)).listar(any(Pageable.class));
    }

    @Test
    void filtrarPorData_shouldPassPageable_andReturnPage() {
        // arrange
        LocalDate inicio = LocalDate.of(2022, 1, 1);
        LocalDate fim = LocalDate.of(2024, 1, 1);
        Pageable expected = PageRequest.of(1, 5, Sort.by("criadoEm").descending());
        Page<Admissao> page = new PageImpl<>(List.of(a1), expected, 1);

        when(service.filtrarPorData(eq(inicio), eq(fim), argThat(p ->
                p.getPageNumber() == 1 && p.getPageSize() == 5 && p.getSort().equals(Sort.by("criadoEm").descending())
        ))).thenReturn(page);

        // act
        ResponseEntity<Page<Admissao>> resp = controller.filtrarPorData(inicio, fim, 1, 5, null);

        // assert
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getContent()).hasSize(1);
        assertThat(resp.getBody().getContent().get(0).getId()).isEqualTo("id-1");

        verify(service, times(1)).filtrarPorData(eq(inicio), eq(fim), any(Pageable.class));
    }

    @Test
    void filtrarPorSalario_shouldApplySortParam_andReturnPage() {
        // arrange
        java.math.BigDecimal min = BigDecimal.valueOf(3000);
        Pageable expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "salarioBruto"));
        Page<Admissao> page = new PageImpl<>(List.of(a2), expected, 1);

        when(service.filtrarPorSalario(eq(min), argThat(p -> p.getSort().equals(Sort.by("salarioBruto").ascending()))))
                .thenReturn(page);

        // act: pass sort param "salarioBruto,asc" and size 10
        ResponseEntity<Page<Admissao>> resp = controller.filtrarPorSalario(min, 0, 10, "salarioBruto,asc");

        // assert
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getContent().get(0).getId()).isEqualTo("id-2");

        verify(service, times(1)).filtrarPorSalario(eq(min), any(Pageable.class));
    }

    // ---------- helpers ----------
    private Admissao buildAdmissao(String id, LocalDate dataAdmissao, BigDecimal salario) {
        return Admissao.builder()
                .id(id)
                .dataAdmissao(dataAdmissao)
                .salarioBruto(salario)
                .anos(1)
                .meses(2)
                .dias(3)
                .porcentagem35(salario.multiply(BigDecimal.valueOf(0.35)))
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
