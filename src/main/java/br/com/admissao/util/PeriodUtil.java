package br.com.admissao.util;

import java.time.LocalDate;
import java.time.Period;

public class PeriodUtil {
    public static PeriodResult calcularPeriodo(LocalDate dataAdmissao) {
        var hoje = LocalDate.now();
        Period p = Period.between(dataAdmissao, hoje);
        return new PeriodResult(p.getYears(), p.getMonths(), p.getDays());
    }
}