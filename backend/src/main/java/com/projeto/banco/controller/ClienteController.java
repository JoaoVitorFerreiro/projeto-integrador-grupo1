package com.projeto.banco.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.banco.dto.ApiException;
import com.projeto.banco.model.ClienteFisica;
import com.projeto.banco.security.AuthInterceptor;
import com.projeto.banco.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ClienteFisica cadastrar(@RequestBody ClienteFisica cliente) {
        return clienteService.cadastrar(cliente);
    }

    /** Listagem geral deixou de ser pública (dados pessoais reais); exige Bearer e é negada. */
    @GetMapping
    public List<ClienteFisica> listar() {
        throw ApiException.forbidden("Listagem de clientes não disponível.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteFisica> buscar(@PathVariable Long id,
            @RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteLogado) {
        if (!id.equals(clienteLogado)) throw ApiException.forbidden("Acesso negado.");
        return clienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
