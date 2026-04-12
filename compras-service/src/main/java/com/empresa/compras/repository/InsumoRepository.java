package com.empresa.compras.repository;

import com.empresa.compras.domain.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByNomeContainingIgnoreCase(String nome);
}
