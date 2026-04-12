package com.empresa.compras.controller;

import com.empresa.compras.domain.Insumo;
import com.empresa.compras.domain.PedidoCompra;
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

    @PutMapping("/insumos/{id}")
    public ResponseEntity<Insumo> atualizarInsumo(@PathVariable Long id, @RequestBody Insumo insumo) {
        insumo.setId(id);
        return ResponseEntity.ok(comprasService.salvarInsumo(insumo));
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

    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<PedidoCompra> atualizarStatus(
            @PathVariable Long id,
            @RequestParam PedidoCompra.StatusPedido status) {
        return ResponseEntity.ok(comprasService.atualizarStatus(id, status));
    }
}
