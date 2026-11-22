package com.projeto.banco.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.banco.dto.Dtos.MeuCupom;
import com.projeto.banco.security.AuthInterceptor;
import com.projeto.banco.service.CupomClienteService;

@RestController
public class MeusCuponsController {

    private final CupomClienteService service;

    public MeusCuponsController(CupomClienteService service) {
        this.service = service;
    }

    @GetMapping("/api/meus-cupons")
    public List<MeuCupom> meusCupons(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId) {
        return service.listarDoCliente(clienteId);
    }
}
