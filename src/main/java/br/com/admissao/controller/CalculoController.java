package br.com.admissao.controller;

import br.com.admissao.dto.CalculoRequestDTO;
import br.com.admissao.dto.CalculoResponseDTO;
import br.com.admissao.model.Admissao;
import br.com.admissao.service.CalculoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/calculos")
@Validated
public class CalculoController {

    private final CalculoService service;

    public CalculoController(CalculoService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<CalculoResponseDTO>> calcular(@Valid @RequestBody CalculoRequestDTO dto) {
        return service.calcularESalvarReactive(dto)
                .map(resp -> ResponseEntity.status(HttpStatus.CREATED).body(resp));
    }

    /**
     * Filtrar por data de admissão com paginação explícita.
     *
     * Query params:
     * - inicio (yyyy-MM-dd) obrigatório
     * - fim (yyyy-MM-dd) obrigatório
     * - page (>=0) default 0
     * - size (>0) default 20
     * - sort (ex: criadoEm,desc) opcional
     */
    @GetMapping("/por-data")
    public ResponseEntity<Page<Admissao>> filtrarPorData(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam("fim") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort, Sort.by("criadoEm").descending());
        Page<Admissao> result = service.filtrarPorData(inicio, fim, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Filtrar por salario mínimo com paginação explícita.
     *
     * Query params:
     * - min (BigDecimal) obrigatório
     * - page, size, sort (mesma lógica)
     */
    @GetMapping("/por-salario")
    public ResponseEntity<Page<Admissao>> filtrarPorSalario(
            @RequestParam("min") java.math.BigDecimal min,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort, Sort.by("salarioBruto").descending());
        Page<Admissao> result = service.filtrarPorSalario(min, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Listar todos com paginação explícita.
     */
    @GetMapping
    public ResponseEntity<Page<Admissao>> listar(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort, Sort.by("criadoEm").descending());
        Page<Admissao> result = service.listar(pageable);
        return ResponseEntity.ok(result);
    }

    // ---------- Helpers ----------
    private Pageable buildPageable(int page, int size, String sortParam, Sort defaultSort) {
        if (sortParam == null || sortParam.isBlank()) {
            return PageRequest.of(page, size, defaultSort);
        }

        // aceita "campo" ou "campo,asc" ou "campo,desc"
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                direction = Sort.Direction.DESC; // fallback silencioso
            }
        }
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
