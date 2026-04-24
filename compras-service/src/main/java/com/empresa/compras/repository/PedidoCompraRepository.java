package com.empresa.compras.repository;

import com.empresa.compras.domain.PedidoCompra;
import com.empresa.compras.domain.enuns.StatusPedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    List<PedidoCompra> findByStatus(StatusPedido status);

    List<PedidoCompra> findByInsumoId(Long insumoId);
}
