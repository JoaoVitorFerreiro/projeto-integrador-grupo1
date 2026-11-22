package com.projeto.banco.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.projeto.banco.model.ClienteFisica;
import com.projeto.banco.repository.ClienteFisicaRepository;

@Service
public class ClienteService {

    private final ClienteFisicaRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ClienteService(ClienteFisicaRepository repository) {
        this.repository = repository;
    }

    /** Cadastro legado: a senha recebida é guardada apenas como hash BCrypt. */
    public ClienteFisica cadastrar(ClienteFisica cliente) {
        if (cliente.getSenha() != null && !cliente.getSenha().isEmpty()) {
            cliente.setSenhaHash(encoder.encode(cliente.getSenha()));
        }
        cliente.setSenha(null);
        cliente.setId(null); // impede sobrescrever contas existentes via id no corpo
        return repository.save(cliente);
    }

    public List<ClienteFisica> listar() {
        return repository.findAll();
    }

    public Optional<ClienteFisica> buscarPorId(Long id) {
        return repository.findById(id);
    }
}
