package com.projeto.banco.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.projeto.banco.model.EstoqueFilial;
import com.projeto.banco.repository.EstoqueFilialRepository;

@Service
public class EstoqueService {

    private final EstoqueFilialRepository repository;

    public EstoqueService(EstoqueFilialRepository repository) {
        this.repository = repository;
    }

    public EstoqueFilial cadastrar(EstoqueFilial estoque) {
        return repository.save(estoque);
    }

    public List<EstoqueFilial> listar() {
        return repository.findAll();
    }

    public List<EstoqueFilial> listarPorFilial(Long filialId) {
        return repository.findByFilialId(filialId);
    }

    public Optional<EstoqueFilial> buscarPorId(Long id) {
        return repository.findById(id);
    }
}
