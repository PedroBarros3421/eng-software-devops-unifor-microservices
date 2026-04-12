package com.empresa.vendas.service;

import com.empresa.vendas.client.ContratosClient;
import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.domain.PedidoVenda;
import com.empresa.vendas.repository.PedidoVendaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendasService {

    private final PedidoVendaRepository pedidoVendaRepository;
    private final ContratosClient contratosClient;

    public List<PedidoVenda> listarTodos() {
        return pedidoVendaRepository.findAll();
    }

    public PedidoVenda buscarPorId(String id) {
        return pedidoVendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido de venda não encontrado: " + id));
    }

    public PedidoVenda criarPedido(PedidoVenda pedido) {
        BigDecimal total = pedido.getItens().stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setValorTotal(total);
        pedido.setStatus(PedidoVenda.StatusPedidoVenda.PENDENTE);
        pedido.setDataPedido(LocalDate.now());
        return pedidoVendaRepository.save(pedido);
    }

    public PedidoVenda atualizarStatus(String id, PedidoVenda.StatusPedidoVenda novoStatus) {
        PedidoVenda pedido = buscarPorId(id);
        pedido.setStatus(novoStatus);
        return pedidoVendaRepository.save(pedido);
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
