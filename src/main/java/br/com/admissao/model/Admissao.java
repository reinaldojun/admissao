package br.com.admissao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admissoes")
public class Admissao {

    @Id
    private String id;

    private LocalDate dataAdmissao;

    private BigDecimal salarioBruto;

    private long dias;
    private long meses;
    private long anos;
    private BigDecimal porcentagem35;

    private LocalDateTime criadoEm;
}

