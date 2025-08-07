package br.com.admissao.service;

import br.com.admissao.dto.CalculoRequestDTO;
import br.com.admissao.dto.CalculoResponseDTO;
import br.com.admissao.exception.ApiException;
import br.com.admissao.model.Admissao;
import br.com.admissao.repository.AdmissaoRepository;
import br.com.admissao.util.PeriodUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Serviço que combina persistência síncrona (MongoRepository) com chamadas reativas ao ViaCEP.
 * Operações bloqueantes (salvar no MongoRepository) são empacotadas em Mono.fromCallable()
 * e executadas em Schedulers.boundedElastic() para evitar bloqueios no event-loop reativo.
 */
@Service
public class CalculoService {

    private final AdmissaoRepository repository;
    private final ViaCepClient viaCepClient;

    public CalculoService(AdmissaoRepository repository, ViaCepClient viaCepClient) {
        this.repository = repository;
        this.viaCepClient = viaCepClient;
    }

    /**
     * Calcula, persiste e retorna o resultado de forma reativa (Mono).
     * - Persiste em uma thread do boundedElastic (porque repository.save é bloqueante).
     * - Chama o ViaCEP de forma reativa (via WebClient) sem bloquear.
     *
     * @param dto dados de entrada
     * @return Mono contendo CalculoResponseDTO
     */
    public Mono<CalculoResponseDTO> calcularESalvarReactive(CalculoRequestDTO dto) {
        var periodo = PeriodUtil.calcularPeriodo(dto.getDataAdmissao());
        BigDecimal porcentagem35 = dto.getSalarioBruto().multiply(BigDecimal.valueOf(0.35));

        Admissao adm = Admissao.builder()
                .dataAdmissao(dto.getDataAdmissao())
                .salarioBruto(dto.getSalarioBruto())
                .anos(periodo.getYears())
                .meses(periodo.getMonths())
                .dias(periodo.getDays())
                .porcentagem35(porcentagem35)
                .criadoEm(LocalDateTime.now())
                .build();

        // Persiste de forma bloqueante em boundedElastic (não bloqueia event-loop)
        Mono<Admissao> salvoMono = Mono.fromCallable(() -> repository.save(adm))
                .subscribeOn(Schedulers.boundedElastic());

        // Integra com ViaCEP (reativo) e monta o DTO sem bloqueios
        return salvoMono.flatMap(salvo ->
                viaCepClient.buscarPorCep(dto.getCep())
                        .switchIfEmpty(Mono.error(new ApiException("ViaCEP não retornou dados para o CEP: " + dto.getCep())))
                        .map(endereco -> CalculoResponseDTO.builder()
                                .id(salvo.getId())
                                .dataAdmissao(salvo.getDataAdmissao())
                                .salarioBruto(salvo.getSalarioBruto())
                                .anos(salvo.getAnos())
                                .meses(salvo.getMeses())
                                .dias(salvo.getDias())
                                .porcentagem35(salvo.getPorcentagem35())
                                .criadoEm(salvo.getCriadoEm())
                                .endereco(endereco)
                                .build()
                        )
        );
    }

    // ---------- Métodos de consulta (sincronos / pageable) ----------
    public Page<Admissao> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Admissao> filtrarPorData(LocalDate inicio, LocalDate fim, Pageable pageable) {
        return repository.findByDataAdmissaoBetween(inicio, fim, pageable);
    }

    public Page<Admissao> filtrarPorSalario(BigDecimal min, Pageable pageable) {
        return repository.findBySalarioBrutoGreaterThanEqual(min, pageable);
    }
}
