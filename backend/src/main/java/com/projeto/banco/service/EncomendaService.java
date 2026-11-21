package com.projeto.banco.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.projeto.banco.model.Encomenda;
import com.projeto.banco.repository.EncomendaRepository;

@Service
public class EncomendaService {

    private final EncomendaRepository repository;

    public EncomendaService(EncomendaRepository repository) {
        this.repository = repository;
    }

    public Encomenda cadastrar(Encomenda encomenda) {
        encomenda.getItens().forEach(i -> {
            i.setEncomenda(encomenda);
            i.calcularSubtotal();
        });
        encomenda.recalcularValorTotal();
        return repository.save(encomenda);
    }

    public List<Encomenda> listar() {
        return repository.findAll();
    }

    public Optional<Encomenda> buscarPorId(Long id) {
        return repository.findById(id);
    }
}
