package com.empresa.compras.service;

import com.empresa.compras.controller.dto.InsumoPatchDTO;
import com.empresa.compras.controller.dto.PedidoPatchInput;
import com.empresa.compras.domain.Insumo;
import com.empresa.compras.domain.PedidoCompra;
import com.empresa.compras.domain.enuns.StatusPedido;
import com.empresa.compras.repository.InsumoRepository;
import com.empresa.compras.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public Insumo atualizarInsumoParcial(Long id, InsumoPatchDTO patch) {
        Insumo existente = buscarInsumoPorId(id);

        if (patch.getNome() != null) {
            existente.setNome(patch.getNome());
        }
        if (patch.getDescricao() != null) {
            existente.setDescricao(patch.getDescricao());
        }
        if (patch.getUnidadeMedida() != null) {
            existente.setUnidadeMedida(patch.getUnidadeMedida());
        }
        if (patch.getPrecoUnitario() != null) {
            existente.setPrecoUnitario(patch.getPrecoUnitario());
        }
        if (patch.getQuantidadeEstoque() != null) {
            existente.setQuantidadeEstoque(patch.getQuantidadeEstoque());
        }

        return insumoRepository.save(existente);
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

        if (pedido.getQuantidade() < 0) {
            throw new RuntimeException("A quantidade deve ser maior que 0");
        }

        pedido.setPrecoTotal(calcularPrecoTotal(pedido.getInsumoId(), pedido.getQuantidade()));
        pedido.setStatus(StatusPedido.PENDENTE);
        pedido.setDataPedido(LocalDate.now());

        return pedidoCompraRepository.save(pedido);
    }

    private BigDecimal calcularPrecoTotal(Long insumoId, int quantidade) {
        Insumo insumo = buscarInsumoPorId(insumoId);
        return insumo.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade));
    }

    @Transactional
    public PedidoCompra atualizarPedido(Long id, PedidoPatchInput patch) {
        
        PedidoCompra pedido = buscarPedidoPorId(id);

        if(pedido.getStatus() == StatusPedido.CANCELADO || pedido.getStatus() == StatusPedido.ENTREGUE){
            throw new RuntimeException("O pedido está cancelado ou entregue não sendo possivel atualizar");
        }

        if (patch.getNomeFornecedor() != null) {
            pedido.setNomeFornecedor(patch.getNomeFornecedor());
        }
        if (patch.getQuantidade() != null) {
            pedido.setQuantidade(patch.getQuantidade());
            pedido.setPrecoTotal(calcularPrecoTotal(pedido.getInsumoId(), patch.getQuantidade()));
        }

        if (patch.getStatus() != null) {
            pedido.setStatus(patch.getStatus());
        }

        if (patch.getDataPedido() != null) {
            pedido.setDataPedido(patch.getDataPedido());
        }
        
        return pedidoCompraRepository.save(pedido);
    }

    @Transactional
    public PedidoCompra atualizarStatus(Long id, StatusPedido novoStatus) {
        PedidoCompra pedido = buscarPedidoPorId(id);

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RuntimeException("O pedido foi cancelado não sendo possivel troca de status");
        }

        if (novoStatus == StatusPedido.ENTREGUE && pedido.getStatus() != StatusPedido.ENTREGUE) {
            if (pedido.getStatus() != StatusPedido.APROVADO) {
                throw new RuntimeException("O pedido deve estar aprovado para ser entregue");
            }

            Insumo insumo = buscarInsumoPorId(pedido.getInsumoId());
            insumo.setQuantidadeEstoque(insumo.getQuantidadeEstoque() + pedido.getQuantidade());
            insumoRepository.save(insumo);
        }

        pedido.setStatus(novoStatus);
        return pedidoCompraRepository.save(pedido);
    }
}
