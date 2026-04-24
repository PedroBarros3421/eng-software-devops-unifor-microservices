package com.empresa.vendas.service;

import com.empresa.vendas.client.ContratosClient;
import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.domain.Pedido;
import com.empresa.vendas.dtos.input.PedidoInputDTO;
import com.empresa.vendas.dtos.output.PedidoOutputDTO;
import com.empresa.vendas.enums.StatusPedidoVenda;
import com.empresa.vendas.repository.PedidoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendasService {

    private final PedidoRepository pedidoRepository;
    private final ContratosClient contratosClient;

    public List<PedidoOutputDTO> listarTodos() {
        List<Pedido> list = pedidoRepository.findAll();
        return list.stream().map(pedido -> new PedidoOutputDTO(
                pedido.getId(),
                pedido.getNomeCliente(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getContratoId(),
                null, // Mapear itens se necessário
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        )).toList();
    }

    public PedidoOutputDTO buscarPorId(UUID id) {
        var pedido = findById(id);
        return new PedidoOutputDTO(
                pedido.getId(),
                pedido.getNomeCliente(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getContratoId(),
                null, // Mapear itens se necessário
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }

    public Pedido findById(UUID id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido de venda não encontrado: " + id));
    }

    public PedidoOutputDTO criarPedido(PedidoInputDTO pedidoInputDTO) {
        BigDecimal total = pedidoInputDTO.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = new Pedido();
        pedido.setValorTotal(total);
        pedido.setStatus(StatusPedidoVenda.PENDENTE);
        pedido.setDataPedido(LocalDate.now());
        pedido = pedidoRepository.save(pedido);

        return new PedidoOutputDTO(
                pedido.getId(),
                pedido.getNomeCliente(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getContratoId(),
                null, // Mapear itens se necessário
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }

    public PedidoOutputDTO atualizarStatus(UUID id, StatusPedidoVenda novoStatus) {
        Pedido pedido = findById(id);
        pedido.setStatus(novoStatus);
        pedido = pedidoRepository.save(pedido);

        return new PedidoOutputDTO(
                pedido.getId(),
                pedido.getNomeCliente(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getContratoId(),
                null, // Mapear itens se necessário
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );

    }

    /**
     * Busca contrato no contratos-service com Circuit Breaker e Retry.
     *
     * - @Retry: tenta até 3 vezes com backoff exponencial antes de acionar o fallback
     * - @CircuitBreaker: abre o disjuntor após 50% de falhas em uma janela de 10 chamadas;
     *   quando aberto, chama diretamente o método fallback sem tentar a chamada remota
     */
    @Retry(name = "contratosService", fallbackMethod = "fallbackContrato")
    @CircuitBreaker(name = "contratosService", fallbackMethod = "fallbackContrato")
    public ContratoDTO consultarContrato(Long contratoId) {
        log.info("Consultando contrato {} no contratos-service", contratoId);
        return contratosClient.buscarContratoPorId(contratoId);
    }

    /**
     * Fallback acionado quando o Circuit Breaker está aberto ou o Retry se esgota.
     * Retorna um DTO vazio indicando indisponibilidade temporária.
     */
    public ContratoDTO fallbackContrato(Long contratoId, Exception ex) {
        log.warn("Fallback acionado para contrato {}. Causa: {}", contratoId, ex.getMessage());
        ContratoDTO fallback = new ContratoDTO();
        fallback.setId(contratoId);
        fallback.setStatus("INDISPONIVEL");
        fallback.setNomeContratante("Serviço temporariamente indisponível");
        return fallback;
    }

    public Optional<ContratoDTO> consultarContratoOpcional(Long contratoId) {
        if (contratoId == null) return Optional.empty();
        ContratoDTO contrato = consultarContrato(contratoId);
        if ("INDISPONIVEL".equals(contrato.getStatus())) return Optional.empty();
        return Optional.of(contrato);
    }
}
