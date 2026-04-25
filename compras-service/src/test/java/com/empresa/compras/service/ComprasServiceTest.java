package com.empresa.compras.service;

import com.empresa.compras.controller.dto.PedidoPatchInput;
import com.empresa.compras.controller.dto.BaixaEstoqueInputDTO;
import com.empresa.compras.controller.dto.BaixaEstoqueResponseDTO;
import com.empresa.compras.controller.dto.DisponibilidadeInsumoDTO;
import com.empresa.compras.domain.Insumo;
import com.empresa.compras.domain.PedidoCompra;
import com.empresa.compras.domain.enums.StatusPedido;
import com.empresa.compras.repository.InsumoRepository;
import com.empresa.compras.repository.PedidoCompraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprasServiceTest {

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private PedidoCompraRepository pedidoCompraRepository;

    @InjectMocks
    private ComprasService comprasService;

    @Test
    void deveAtualizarEstoqueQuandoPedidoAprovadoForEntregue() {
        PedidoCompra pedido = novoPedido(StatusPedido.APROVADO, 5);
        Insumo insumo = novoInsumo(10);

        when(pedidoCompraRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(insumoRepository.findById(100L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoCompra atualizado = comprasService.atualizarStatus(1L, StatusPedido.ENTREGUE);

        assertEquals(StatusPedido.ENTREGUE, atualizado.getStatus());
        assertEquals(15, insumo.getQuantidadeEstoque());
    }

    @Test
    void naoDevePermitirEntregaSemAprovacaoPrevia() {
        PedidoCompra pedido = novoPedido(StatusPedido.PENDENTE, 5);

        when(pedidoCompraRepository.findById(1L)).thenReturn(Optional.of(pedido));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> comprasService.atualizarStatus(1L, StatusPedido.ENTREGUE));

        assertEquals("O pedido deve estar aprovado para ser entregue", ex.getMessage());
        verify(insumoRepository, never()).save(any(Insumo.class));
    }

    @Test
    void patchDePedidoNaoDeveAlterarStatus() {
        PedidoCompra pedido = novoPedido(StatusPedido.PENDENTE, 5);
        PedidoPatchInput patch = new PedidoPatchInput();
        patch.setNomeFornecedor("Fornecedor Atualizado");
        patch.setQuantidade(8);
        patch.setDataPedido(LocalDate.now().plusDays(1));

        Insumo insumo = novoInsumo(10);

        when(pedidoCompraRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(insumoRepository.findById(100L)).thenReturn(Optional.of(insumo));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoCompra atualizado = comprasService.atualizarPedido(1L, patch);

        assertEquals(StatusPedido.PENDENTE, atualizado.getStatus());
        assertEquals("Fornecedor Atualizado", atualizado.getNomeFornecedor());
        assertEquals(8, atualizado.getQuantidade());
        assertEquals(new BigDecimal("80.00"), atualizado.getPrecoTotal());
    }

    @Test
    void deveRetornarDisponibilidadeQuandoHouverEstoqueSuficiente() {
        Insumo insumo = novoInsumo(10);
        when(insumoRepository.findById(100L)).thenReturn(Optional.of(insumo));

        DisponibilidadeInsumoDTO response = comprasService.consultarDisponibilidade(100L, 4);

        assertEquals(100L, response.insumoId());
        assertEquals(true, response.disponivel());
        assertEquals(4, response.quantidadeSolicitada());
        assertEquals(10, response.quantidadeEstoque());
    }

    @Test
    void deveBaixarEstoqueQuandoQuantidadeForValida() {
        Insumo insumo = novoInsumo(10);
        BaixaEstoqueInputDTO input = new BaixaEstoqueInputDTO();
        input.setQuantidade(3);

        when(insumoRepository.findById(100L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaixaEstoqueResponseDTO response = comprasService.baixarEstoque(100L, input);

        assertEquals(100L, response.insumoId());
        assertEquals(3, response.quantidadeBaixada());
        assertEquals(7, response.quantidadeEstoqueAtual());
    }

    @Test
    void deveFalharQuandoBaixaDeEstoqueNaoTiverQuantidadeSuficiente() {
        Insumo insumo = novoInsumo(2);
        BaixaEstoqueInputDTO input = new BaixaEstoqueInputDTO();
        input.setQuantidade(3);

        when(insumoRepository.findById(100L)).thenReturn(Optional.of(insumo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> comprasService.baixarEstoque(100L, input));

        assertEquals("Estoque insuficiente para o insumo: 100", ex.getMessage());
    }

    private PedidoCompra novoPedido(StatusPedido status, int quantidade) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.setId(1L);
        pedido.setNomeFornecedor("Fornecedor");
        pedido.setInsumoId(100L);
        pedido.setQuantidade(quantidade);
        pedido.setPrecoTotal(new BigDecimal("50.00"));
        pedido.setStatus(status);
        pedido.setDataPedido(LocalDate.now());
        return pedido;
    }

    private Insumo novoInsumo(int estoque) {
        Insumo insumo = new Insumo();
        insumo.setId(100L);
        insumo.setNome("Insumo");
        insumo.setDescricao("Descricao");
        insumo.setUnidadeMedida("UN");
        insumo.setPrecoUnitario(new BigDecimal("10.00"));
        insumo.setQuantidadeEstoque(estoque);
        return insumo;
    }
}
