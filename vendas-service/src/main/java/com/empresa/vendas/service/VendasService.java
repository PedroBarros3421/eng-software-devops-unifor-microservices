package com.empresa.vendas.service;

import com.empresa.vendas.client.ContratosClient;
import com.empresa.vendas.client.ComprasClient;
import com.empresa.vendas.client.dto.BaixaEstoqueInputDTO;
import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.client.dto.ContratoStatusResponseDTO;
import com.empresa.vendas.client.dto.DisponibilidadeInsumoDTO;
import com.empresa.vendas.domain.Item;
import com.empresa.vendas.domain.Pedido;
import com.empresa.vendas.dtos.input.ItemInputDTO;
import com.empresa.vendas.dtos.input.PedidoInputDTO;
import com.empresa.vendas.dtos.output.ItemOutputDTO;
import com.empresa.vendas.dtos.output.PedidoOutputDTO;
import com.empresa.vendas.enums.StatusPedidoVenda;
import com.empresa.vendas.exception.BusinessException;
import com.empresa.vendas.exception.ResourceNotFoundException;
import com.empresa.vendas.repository.PedidoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendasService {

    private final PedidoRepository pedidoRepository;
    private final ContratosClient contratosClient;
    private final ComprasClient comprasClient;

    @Transactional(readOnly = true)
    public List<PedidoOutputDTO> listarTodos() {
        List<Pedido> list = pedidoRepository.findAll();
        return list.stream().map(this::toOutputDTO).toList();
    }

    @Transactional(readOnly = true)
    public PedidoOutputDTO buscarPorId(UUID id) {
        var pedido = findById(id);
        return toOutputDTO(pedido);
    }

    @Transactional(readOnly = true)
    public Pedido findById(UUID id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda não encontrado: " + id));
    }

    @Transactional
    public PedidoOutputDTO criarPedido(PedidoInputDTO pedidoInputDTO) {
        ContratoStatusResponseDTO contratoStatusResponseDTO = contratosClient.validarContrato(pedidoInputDTO.contratoId());
        if (nonNull(contratoStatusResponseDTO) && !contratoStatusResponseDTO.valido())
            throw new BusinessException("Contrato inválido ou inativo: " + pedidoInputDTO.contratoId());

        if (isNull(contratoStatusResponseDTO))
            throw new BusinessException("Não foi possível validar o contrato: " + pedidoInputDTO.contratoId());

        List<ItemInputDTO> itensInput = nonNull(pedidoInputDTO.itens()) ? pedidoInputDTO.itens() : Collections.emptyList();
        if (itensInput.isEmpty()) {
            throw new BusinessException("O pedido deve possuir ao menos um item");
        }

        for (ItemInputDTO item : itensInput) {
            DisponibilidadeInsumoDTO disponibilidade = comprasClient.consultarDisponibilidade(item.insumoId(), item.quantidade());
            if (isNull(disponibilidade)) {
                throw new BusinessException("Não foi possível validar o estoque do insumo: " + item.insumoId());
            }
            if (!disponibilidade.disponivel()) {
                throw new BusinessException("Estoque insuficiente para o insumo: " + item.insumoId());
            }
        }

        for (ItemInputDTO item : itensInput) {
            comprasClient.baixarEstoque(item.insumoId(), new BaixaEstoqueInputDTO(item.quantidade()));
        }

        BigDecimal total = itensInput.stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = new Pedido();
        pedido.setValorTotal(total);
        pedido.setStatus(StatusPedidoVenda.PENDENTE);
        pedido.setDataPedido(nonNull(pedidoInputDTO.dataPedido()) ? pedidoInputDTO.dataPedido() : LocalDate.now());
        pedido.setNomeCliente(pedidoInputDTO.nomeCliente());
        pedido.setContratoId(pedidoInputDTO.contratoId());
        pedido.setItens(itensInput.stream().map(item -> toEntity(item, pedido)).toList());
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        return toOutputDTO(pedidoSalvo);
    }

    @Transactional
    public PedidoOutputDTO atualizarStatus(UUID id, StatusPedidoVenda novoStatus) {
        Pedido pedido = findById(id);
        pedido.setStatus(novoStatus);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        return toOutputDTO(pedidoSalvo);
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

    private PedidoOutputDTO toOutputDTO(Pedido pedido) {
        return new PedidoOutputDTO(
                pedido.getId(),
                pedido.getNomeCliente(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getContratoId(),
                nonNull(pedido.getItens()) ? pedido.getItens().stream().map(this::toOutputDTO).toList() : List.of(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }

    private ItemOutputDTO toOutputDTO(Item item) {
        return new ItemOutputDTO(
                item.getId(),
                item.getInsumoId(),
                item.getNomeInsumo(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getDataCriacao(),
                item.getDataAtualizacao()
        );
    }

    private Item toEntity(ItemInputDTO itemInputDTO, Pedido pedido) {
        Item item = new Item();
        item.setInsumoId(itemInputDTO.insumoId());
        item.setNomeInsumo(itemInputDTO.nomeInsumo());
        item.setQuantidade(itemInputDTO.quantidade());
        item.setPrecoUnitario(itemInputDTO.precoUnitario());
        item.setPedido(pedido);
        return item;
    }
}
