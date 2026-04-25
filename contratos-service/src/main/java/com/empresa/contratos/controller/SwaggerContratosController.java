package com.empresa.contratos.controller;

import com.empresa.contratos.domain.Contrato;
import com.empresa.contratos.dto.ContratoStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Contratos", description = "Endpoints para cadastro, atualização e validação de contratos")
public interface SwaggerContratosController extends SwaggerCommon {

    @Operation(summary = "Listar contratos", description = "Lista todos os contratos ou apenas os ativos")
    List<Contrato> listarTodos(@Parameter(description = "Quando true, retorna apenas contratos ativos") Boolean ativos);

    @Operation(summary = "Buscar contrato por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado")
    })
    ResponseEntity<Contrato> buscarPorId(Long id);

    @Operation(summary = "Buscar contrato por número")
    ResponseEntity<Contrato> buscarPorNumero(String numero);

    @Operation(summary = "Criar contrato")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    ResponseEntity<Contrato> criar(Contrato contrato);

    @Operation(summary = "Atualizar contrato")
    ResponseEntity<Contrato> atualizar(Long id, Contrato contrato);

    @Operation(summary = "Atualizar status do contrato")
    ResponseEntity<Contrato> atualizarStatus(Long id, Contrato.StatusContrato status);

    @Operation(summary = "Excluir contrato")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrato excluído")
    })
    ResponseEntity<Void> excluir(Long id);

    @Operation(summary = "Validar contrato")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validação executada")
    })
    ResponseEntity<ContratoStatusDTO> validarContrato(Long id);
}
