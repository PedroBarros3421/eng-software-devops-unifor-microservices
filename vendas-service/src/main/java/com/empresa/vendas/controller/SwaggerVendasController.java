package com.empresa.vendas.controller;

import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.dtos.input.PedidoInputDTO;
import com.empresa.vendas.dtos.output.PedidoOutputDTO;
import com.empresa.vendas.enums.StatusPedidoVenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Vendas Controller", description = "Endpoints relacionados a vendas")
public interface SwaggerVendasController extends SwaggerCommon {

    @Operation(summary = "Criar pedido", description = "Cria um novo pedido de venda")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    ResponseEntity<PedidoOutputDTO> criarPedido(PedidoInputDTO pedidoInputDTO);

    @Operation(summary = "Listar pedidos", description = "Retorna a lista de pedidos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedidos listados com sucesso")
    })
    List<PedidoOutputDTO> listarPedidos();

    @Operation(summary = "Buscar pedido por ID", description = "Retorna os dados de um pedido específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    ResponseEntity<PedidoOutputDTO> buscarPedido(UUID id);

    @Operation(summary = "Atualizar status do pedido", description = "Atualiza o status de um pedido de venda")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    ResponseEntity<PedidoOutputDTO> atualizarStatus(UUID id, StatusPedidoVenda status);

    @Operation(summary = "Consultar contrato", description = "Consulta um contrato vinculado via contratos-service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contrato consultado com sucesso"),
            @ApiResponse(responseCode = "503", description = "Serviço de contratos indisponível")
    })
    ResponseEntity<ContratoDTO> consultarContrato(Long contratoId);
}
