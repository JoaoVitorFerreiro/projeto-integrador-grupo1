package com.projeto.banco.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.projeto.banco.model.ClienteFisica;
import com.projeto.banco.repository.ClienteFisicaRepository;

@Service
public class ClienteService {

    private final ClienteFisicaRepository repository;

    public ClienteService(ClienteFisicaRepository repository) {
        this.repository = repository;
    }

    public ClienteFisica cadastrar(ClienteFisica cliente) {
        return repository.save(cliente);
    }

    public List<ClienteFisica> listar() {
        return repository.findAll();
    }

    public Optional<ClienteFisica> buscarPorId(Long id) {
        return repository.findById(id);
    }
}
