package com.empresa.compras.service;

import com.empresa.compras.domain.Insumo;
import com.empresa.compras.domain.PedidoCompra;
import com.empresa.compras.repository.InsumoRepository;
import com.empresa.compras.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComprasService {

    private final InsumoRepository insumoRepository;
    private final PedidoCompraRepository pedidoCompraRepository;

    public List<Insumo> listarInsumos() {
        return insumoRepository.findAll();
    }

    public Insumo buscarInsumoPorId(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo não encontrado: " + id));
    }

    @Transactional
    public Insumo salvarInsumo(Insumo insumo) {
        return insumoRepository.save(insumo);
    }

    @Transactional
    public void excluirInsumo(Long id) {
        insumoRepository.deleteById(id);
    }

    public List<PedidoCompra> listarPedidos() {
        return pedidoCompraRepository.findAll();
    }

    public PedidoCompra buscarPedidoPorId(Long id) {
        return pedidoCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido de compra não encontrado: " + id));
    }

    @Transactional
    public PedidoCompra criarPedido(PedidoCompra pedido) {
        Insumo insumo = buscarInsumoPorId(pedido.getInsumoId());
        pedido.setPrecoTotal(insumo.getPrecoUnitario().multiply(
                java.math.BigDecimal.valueOf(pedido.getQuantidade())));
        pedido.setStatus(PedidoCompra.StatusPedido.PENDENTE);
        pedido.setDataPedido(LocalDate.now());
        return pedidoCompraRepository.save(pedido);
    }

    @Transactional
    public PedidoCompra atualizarStatus(Long id, PedidoCompra.StatusPedido novoStatus) {
        PedidoCompra pedido = buscarPedidoPorId(id);

        if (novoStatus == PedidoCompra.StatusPedido.ENTREGUE) {
            Insumo insumo = buscarInsumoPorId(pedido.getInsumoId());
            insumo.setQuantidadeEstoque(insumo.getQuantidadeEstoque() + pedido.getQuantidade());
            insumoRepository.save(insumo);
        }

        pedido.setStatus(novoStatus);
        return pedidoCompraRepository.save(pedido);
    }
}
