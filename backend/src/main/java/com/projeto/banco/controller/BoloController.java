package com.projeto.banco.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.banco.service.BoloService;

@RestController
public class BoloController {

    private final BoloService service;

    public BoloController(BoloService service) {
        this.service = service;
    }

    @GetMapping("/api/bolos/modelos")
    public Map<String, List<String>> modelos() {
        return Map.of("modelos", service.listarModelos());
    }
}
