package com.projeto.banco.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.banco.dto.Dtos.AvaliacaoPublica;
import com.projeto.banco.dto.Dtos.AvaliacaoRequest;
import com.projeto.banco.dto.Dtos.AvaliacaoResponse;
import com.projeto.banco.security.AuthInterceptor;
import com.projeto.banco.service.AvaliacaoService;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId,
                                                   @RequestBody AvaliacaoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(clienteId, req));
    }

    @GetMapping
    public List<AvaliacaoPublica> listar() {
        return service.listarPublicas();
    }
}
