package com.empresa.vendas.repository;

import com.empresa.vendas.domain.PedidoVenda;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoVendaRepository extends MongoRepository<PedidoVenda, String> {

    List<PedidoVenda> findByStatus(PedidoVenda.StatusPedidoVenda status);

    List<PedidoVenda> findByNomeClienteContainingIgnoreCase(String nomeCliente);

    List<PedidoVenda> findByContratoId(Long contratoId);
}
