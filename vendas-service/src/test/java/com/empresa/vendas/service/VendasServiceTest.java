package com.empresa.vendas.service;

import com.empresa.vendas.client.ContratosClient;
import com.empresa.vendas.client.dto.ContratoStatusResponseDTO;
import com.empresa.vendas.domain.Pedido;
import com.empresa.vendas.dtos.input.ItemInputDTO;
import com.empresa.vendas.dtos.input.PedidoInputDTO;
import com.empresa.vendas.dtos.output.PedidoOutputDTO;
import com.empresa.vendas.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendasServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ContratosClient contratosClient;

    @InjectMocks
    private VendasService vendasService;

    @Test
    void deveCriarPedidoComItensEDataDoInput() {
        PedidoInputDTO input = new PedidoInputDTO(
                "Cliente Exemplo",
                LocalDate.of(2026, 4, 25),
                10L,
                List.of(
                        new ItemInputDTO(1L, "Item A", 2, new BigDecimal("10.00")),
                        new ItemInputDTO(2L, "Item B", 1, new BigDecimal("5.00"))
                )
        );

        when(contratosClient.validarContrato(10L))
                .thenReturn(new ContratoStatusResponseDTO(10L, true, "ATIVO", null));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(UUID.randomUUID());
            pedido.setDataCriacao(LocalDateTime.now());
            pedido.setDataAtualizacao(LocalDateTime.now());
            return pedido;
        });

        PedidoOutputDTO output = vendasService.criarPedido(input);

        assertEquals("Cliente Exemplo", output.nomeCliente());
        assertEquals(new BigDecimal("25.00"), output.valorTotal());
        assertEquals(LocalDate.of(2026, 4, 25), output.dataPedido());
        assertNotNull(output.itens());
        assertEquals(2, output.itens().size());

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido salvo = captor.getValue();
        assertEquals(2, salvo.getItens().size());
        assertEquals(salvo, salvo.getItens().get(0).getPedido());
        assertEquals(salvo, salvo.getItens().get(1).getPedido());
    }

    @Test
    void deveUsarDataAtualQuandoInputNaoInformarDataPedido() {
        LocalDate hoje = LocalDate.now();
        PedidoInputDTO input = new PedidoInputDTO(
                "Cliente Exemplo",
                null,
                10L,
                List.of(new ItemInputDTO(1L, "Item A", 1, new BigDecimal("10.00")))
        );

        when(contratosClient.validarContrato(10L))
                .thenReturn(new ContratoStatusResponseDTO(10L, true, "ATIVO", null));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(UUID.randomUUID());
            return pedido;
        });

        PedidoOutputDTO output = vendasService.criarPedido(input);

        assertEquals(hoje, output.dataPedido());
    }

    @Test
    void deveFalharQuandoContratoForInvalido() {
        PedidoInputDTO input = new PedidoInputDTO(
                "Cliente Exemplo",
                null,
                99L,
                List.of(new ItemInputDTO(1L, "Item A", 1, new BigDecimal("10.00")))
        );

        when(contratosClient.validarContrato(99L))
                .thenReturn(new ContratoStatusResponseDTO(99L, false, "SUSPENSO", "Contrato não está ativo"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendasService.criarPedido(input));

        assertEquals("Contrato inválido ou inativo: 99", ex.getMessage());
    }
}
