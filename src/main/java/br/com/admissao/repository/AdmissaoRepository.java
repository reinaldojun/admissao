package br.com.admissao.repository;

import br.com.admissao.model.Admissao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface AdmissaoRepository extends MongoRepository<Admissao, String> {

    Page<Admissao> findByDataAdmissaoBetween(LocalDate inicio, LocalDate fim, Pageable pageable);

    Page<Admissao> findBySalarioBrutoGreaterThanEqual(BigDecimal salarioMinimo, Pageable pageable);

}
