package com.empresa.contratos.repository;

import com.empresa.contratos.domain.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    Optional<Contrato> findByNumero(String numero);

    List<Contrato> findByStatus(Contrato.StatusContrato status);

    List<Contrato> findByNomeContratanteContainingIgnoreCase(String nomeContratante);
}
