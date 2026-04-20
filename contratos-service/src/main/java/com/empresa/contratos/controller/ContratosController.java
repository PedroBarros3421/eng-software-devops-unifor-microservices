package com.empresa.contratos.controller;

import com.empresa.contratos.domain.Contrato;
import com.empresa.contratos.dto.ContratoStatusDTO;
import com.empresa.contratos.service.ContratosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratosController {

    private final ContratosService contratosService;

    @GetMapping
    public List<Contrato> listarTodos(@RequestParam(required = false) Boolean ativos) {
        if (Boolean.TRUE.equals(ativos)) {
            return contratosService.listarAtivos();
        }
        return contratosService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrato> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratosService.buscarPorId(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<Contrato> buscarPorNumero(@PathVariable String numero) {
        return ResponseEntity.ok(contratosService.buscarPorNumero(numero));
    }

    @PostMapping
    public ResponseEntity<Contrato> criar(@RequestBody Contrato contrato) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratosService.salvar(contrato));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contrato> atualizar(@PathVariable Long id, @RequestBody Contrato contrato) {
        contrato.setId(id);
        return ResponseEntity.ok(contratosService.salvar(contrato));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Contrato> atualizarStatus(
            @PathVariable Long id,
            @RequestParam Contrato.StatusContrato status) {
        return ResponseEntity.ok(contratosService.atualizarStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        contratosService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/validacao")
    public ResponseEntity<ContratoStatusDTO> validarContrato(@PathVariable Long id){
        return ResponseEntity.ok(contratosService.validarContrato(id));
    }
}
