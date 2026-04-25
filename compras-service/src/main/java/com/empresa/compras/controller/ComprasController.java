package com.empresa.compras.controller;

import com.empresa.compras.controller.dto.InsumoPatchDTO;
import com.empresa.compras.controller.dto.BaixaEstoqueInputDTO;
import com.empresa.compras.controller.dto.BaixaEstoqueResponseDTO;
import com.empresa.compras.controller.dto.DisponibilidadeInsumoDTO;
import com.empresa.compras.controller.dto.PedidoPatchInput;
import com.empresa.compras.domain.Insumo;
import com.empresa.compras.domain.PedidoCompra;
import com.empresa.compras.domain.enums.StatusPedido;
import com.empresa.compras.service.ComprasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class ComprasController {

    private final ComprasService comprasService;

    // --- Insumos ---

    @GetMapping("/insumos")
    public List<Insumo> listarInsumos() {
        return comprasService.listarInsumos();
    }

    @GetMapping("/insumos/{id}")
    public ResponseEntity<Insumo> buscarInsumo(@PathVariable Long id) {
        return ResponseEntity.ok(comprasService.buscarInsumoPorId(id));
    }

    @PostMapping("/insumos")
    public ResponseEntity<Insumo> criarInsumo(@RequestBody Insumo insumo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comprasService.salvarInsumo(insumo));
    }

    @PatchMapping("/insumos/{id}")
    public ResponseEntity<Insumo> atualizarInsumo(@PathVariable Long id, @RequestBody InsumoPatchDTO patch) {
        return ResponseEntity.ok(comprasService.atualizarInsumoParcial(id, patch));
    }

    @GetMapping("/insumos/{id}/disponibilidade")
    public ResponseEntity<DisponibilidadeInsumoDTO> consultarDisponibilidade(
            @PathVariable Long id,
            @RequestParam Integer quantidade) {
        return ResponseEntity.ok(comprasService.consultarDisponibilidade(id, quantidade));
    }

    @PostMapping("/insumos/{id}/baixa")
    public ResponseEntity<BaixaEstoqueResponseDTO> baixarEstoque(
            @PathVariable Long id,
            @RequestBody BaixaEstoqueInputDTO input) {
        return ResponseEntity.ok(comprasService.baixarEstoque(id, input));
    }

    @DeleteMapping("/insumos/{id}")
    public ResponseEntity<Void> excluirInsumo(@PathVariable Long id) {
        comprasService.excluirInsumo(id);
        return ResponseEntity.noContent().build();
    }

    // --- Pedidos de Compra ---

    @GetMapping("/pedidos")
    public List<PedidoCompra> listarPedidos() {
        return comprasService.listarPedidos();
    }

    @GetMapping("/pedidos/{id}")
    public ResponseEntity<PedidoCompra> buscarPedido(@PathVariable Long id) {
        return ResponseEntity.ok(comprasService.buscarPedidoPorId(id));
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoCompra> criarPedido(@RequestBody PedidoCompra pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comprasService.criarPedido(pedido));
    }

    @PatchMapping("/pedidos/{id}")
    public ResponseEntity<PedidoCompra> atualizarPedido(@PathVariable Long id, @RequestBody PedidoPatchInput patch) {
        return ResponseEntity.ok(comprasService.atualizarPedido(id, patch));
    }

    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<PedidoCompra> atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusPedido status) {
        return ResponseEntity.ok(comprasService.atualizarStatus(id, status));
    }
}
