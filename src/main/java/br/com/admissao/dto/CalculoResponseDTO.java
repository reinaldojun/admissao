package br.com.admissao.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoResponseDTO {
    private String id;
    private LocalDate dataAdmissao;
    private BigDecimal salarioBruto;
    private long anos;
    private long meses;
    private long dias;
    private BigDecimal porcentagem35;
    private LocalDateTime criadoEm;
    private ViaCepDTO endereco;
}
