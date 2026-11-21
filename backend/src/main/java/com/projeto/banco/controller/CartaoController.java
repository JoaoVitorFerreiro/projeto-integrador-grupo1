package com.projeto.banco.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projeto.banco.service.CartaoService;
import com.projeto.banco.model.Cartao;

@RestController
@RequestMapping("/api/cartoes")
public class CartaoController {

    private final CartaoService cartaoService;

    public CartaoController(CartaoService cartaoService) {
        this.cartaoService = cartaoService;
    }

    @PostMapping
    public Cartao cadastrar(@RequestBody Cartao cartao) {
        return cartaoService.cadastrar(cartao);
    }

    @GetMapping
    public List<Cartao> listar() {
        return cartaoService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cartao> buscar(@PathVariable Long id) {
        return cartaoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}