package br.com.admissao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Requisição para calcular tempo e porcentagem salarial")
public class CalculoRequestDTO {

    @NotNull(message = "dataAdmissao é obrigatória")
    @PastOrPresent(message = "dataAdmissao não pode ser no futuro")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de admissão (yyyy-MM-dd)", example = "2022-05-10", required = true)
    private LocalDate dataAdmissao;

    @NotNull(message = "salarioBruto é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "salarioBruto deve ser maior que zero")
    @Schema(description = "Salário bruto", example = "3500.00", required = true)
    private BigDecimal salarioBruto;

    @NotBlank(message = "cep é obrigatório")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido. Formato esperado: 12345-678 ou 12345678")
    @Schema(description = "CEP (12345-678 ou 12345678)", example = "66050080", required = true)
    private String cep;
}