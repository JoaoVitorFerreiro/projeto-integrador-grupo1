package com.projeto.banco.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projeto.banco.model.EstoqueFilial;
import com.projeto.banco.service.EstoqueService;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @PostMapping
    public EstoqueFilial cadastrar(@RequestBody EstoqueFilial estoque) {
        return estoqueService.cadastrar(estoque);
    }

    @GetMapping
    public List<EstoqueFilial> listar(@RequestParam(required = false) Long filialId) {
        return filialId == null ? estoqueService.listar() : estoqueService.listarPorFilial(filialId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstoqueFilial> buscar(@PathVariable Long id) {
        return estoqueService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
