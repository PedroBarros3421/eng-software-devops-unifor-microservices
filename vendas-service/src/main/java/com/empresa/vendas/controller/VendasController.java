package com.empresa.vendas.controller;

import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.dtos.input.PedidoInputDTO;
import com.empresa.vendas.dtos.output.PedidoOutputDTO;
import com.empresa.vendas.enums.StatusPedidoVenda;
import com.empresa.vendas.service.VendasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendasController implements SwaggerVendasController {

    private final VendasService vendasService;

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoOutputDTO> criarPedido(@RequestBody PedidoInputDTO pedidoInputDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendasService.criarPedido(pedidoInputDTO));
    }

    @GetMapping("/pedidos")
    public List<PedidoOutputDTO> listarPedidos() {
        return vendasService.listarTodos();
    }

    @GetMapping("/pedidos/{id}")
    public ResponseEntity<PedidoOutputDTO> buscarPedido(@PathVariable UUID id) {
        return ResponseEntity.ok(vendasService.buscarPorId(id));
    }


    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<PedidoOutputDTO> atualizarStatus(
            @PathVariable UUID id,
            @RequestParam StatusPedidoVenda status) {
        return ResponseEntity.ok(vendasService.atualizarStatus(id, status));
    }

    /**
     * Consulta contrato vinculado ao pedido via Feign Client.
     * Demonstra o Circuit Breaker e o Fallback em ação.
     */
    @GetMapping("/contratos/{contratoId}")
    public ResponseEntity<ContratoDTO> consultarContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(vendasService.consultarContrato(contratoId));
    }
}
