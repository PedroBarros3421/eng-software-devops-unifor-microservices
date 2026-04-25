package com.empresa.compras.controller;

import com.empresa.compras.controller.dto.BaixaEstoqueInputDTO;
import com.empresa.compras.controller.dto.BaixaEstoqueResponseDTO;
import com.empresa.compras.controller.dto.DisponibilidadeInsumoDTO;
import com.empresa.compras.controller.dto.InsumoPatchDTO;
import com.empresa.compras.controller.dto.PedidoPatchInput;
import com.empresa.compras.domain.Insumo;
import com.empresa.compras.domain.PedidoCompra;
import com.empresa.compras.domain.enums.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Compras", description = "Endpoints para estoque e pedidos de compra")
public interface SwaggerComprasController extends SwaggerCommon {

    @Operation(summary = "Listar insumos")
    List<Insumo> listarInsumos();

    @Operation(summary = "Buscar insumo por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Insumo encontrado"),
            @ApiResponse(responseCode = "404", description = "Insumo não encontrado")
    })
    ResponseEntity<Insumo> buscarInsumo(@Parameter(description = "ID do insumo") Long id);

    @Operation(summary = "Criar insumo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Insumo criado"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    ResponseEntity<Insumo> criarInsumo(Insumo insumo);

    @Operation(summary = "Atualizar parcialmente um insumo")
    ResponseEntity<Insumo> atualizarInsumo(Long id, InsumoPatchDTO patch);

    @Operation(summary = "Consultar disponibilidade de um insumo")
    ResponseEntity<DisponibilidadeInsumoDTO> consultarDisponibilidade(Long id, Integer quantidade);

    @Operation(summary = "Baixar estoque de um insumo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque baixado"),
            @ApiResponse(responseCode = "422", description = "Estoque insuficiente")
    })
    ResponseEntity<BaixaEstoqueResponseDTO> baixarEstoque(Long id, BaixaEstoqueInputDTO input);

    @Operation(summary = "Excluir insumo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Insumo excluído")
    })
    ResponseEntity<Void> excluirInsumo(Long id);

    @Operation(summary = "Listar pedidos de compra")
    List<PedidoCompra> listarPedidos();

    @Operation(summary = "Buscar pedido de compra por ID")
    ResponseEntity<PedidoCompra> buscarPedido(Long id);

    @Operation(summary = "Criar pedido de compra")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    ResponseEntity<PedidoCompra> criarPedido(PedidoCompra pedido);

    @Operation(summary = "Atualizar parcialmente um pedido de compra")
    ResponseEntity<PedidoCompra> atualizarPedido(Long id, PedidoPatchInput patch);

    @Operation(summary = "Atualizar status de um pedido de compra")
    ResponseEntity<PedidoCompra> atualizarStatus(Long id, StatusPedido status);
}
