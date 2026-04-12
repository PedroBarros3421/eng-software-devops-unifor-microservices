package com.empresa.vendas.controller;

import com.empresa.vendas.client.dto.ContratoDTO;
import com.empresa.vendas.domain.PedidoVenda;
import com.empresa.vendas.service.VendasService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendasController {

    private final VendasService vendasService;

    @GetMapping("/pedidos")
    public List<PedidoVenda> listarPedidos() {
        return vendasService.listarTodos();
    }

    @GetMapping("/pedidos/{id}")
    public ResponseEntity<PedidoVenda> buscarPedido(@PathVariable String id) {
        return ResponseEntity.ok(vendasService.buscarPorId(id));
    }

    /**
     * Criação de pedido com Rate Limiter para proteger o endpoint de sobrecarga.
     * Configurado para aceitar até 10 requisições por segundo (ver application.yml).
     */
    @PostMapping("/pedidos")
    @RateLimiter(name = "vendas-api")
    public ResponseEntity<PedidoVenda> criarPedido(@RequestBody PedidoVenda pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendasService.criarPedido(pedido));
    }

    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<PedidoVenda> atualizarStatus(
            @PathVariable String id,
            @RequestParam PedidoVenda.StatusPedidoVenda status) {
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
