package com.projeto.banco.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.banco.dto.Dtos.MensagemRequest;
import com.projeto.banco.dto.Dtos.MensagemResponse;
import com.projeto.banco.dto.Dtos.PedidoRequest;
import com.projeto.banco.dto.Dtos.PedidoResponse;
import com.projeto.banco.security.AuthInterceptor;
import com.projeto.banco.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId,
                                                @RequestBody PedidoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(clienteId, req));
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId) {
        return service.listar(clienteId);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId,
                                 @PathVariable String id) {
        return service.buscar(clienteId, id);
    }

    @GetMapping("/{id}/mensagens")
    public List<MensagemResponse> mensagens(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId,
                                            @PathVariable String id) {
        return service.mensagens(clienteId, id);
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<MensagemResponse> enviar(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId,
                                                   @PathVariable String id, @RequestBody MensagemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.enviarMensagem(clienteId, id, req == null ? null : req.texto()));
    }
}
