package com.empresa.contratos.service;

import com.empresa.contratos.domain.Contrato;
import com.empresa.contratos.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratosService {

    private final ContratoRepository contratoRepository;

    public List<Contrato> listarTodos() {
        return contratoRepository.findAll();
    }

    public List<Contrato> listarAtivos() {
        return contratoRepository.findByStatus(Contrato.StatusContrato.ATIVO);
    }

    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado: " + id));
    }

    public Contrato buscarPorNumero(String numero) {
        return contratoRepository.findByNumero(numero)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado: " + numero));
    }

    @Transactional
    public Contrato salvar(Contrato contrato) {
        return contratoRepository.save(contrato);
    }

    @Transactional
    public Contrato atualizarStatus(Long id, Contrato.StatusContrato novoStatus) {
        Contrato contrato = buscarPorId(id);
        contrato.setStatus(novoStatus);
        return contratoRepository.save(contrato);
    }

    @Transactional
    public void excluir(Long id) {
        contratoRepository.deleteById(id);
    }
}
