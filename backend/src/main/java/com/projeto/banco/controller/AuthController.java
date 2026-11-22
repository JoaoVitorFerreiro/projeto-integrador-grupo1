package com.projeto.banco.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.banco.dto.Dtos.AuthResponse;
import com.projeto.banco.dto.Dtos.LoginRequest;
import com.projeto.banco.dto.Dtos.RegisterRequest;
import com.projeto.banco.dto.Dtos.UsuarioResponse;
import com.projeto.banco.security.AuthInterceptor;
import com.projeto.banco.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public UsuarioResponse me(@RequestAttribute(AuthInterceptor.ATTR_CLIENTE_ID) Long clienteId) {
        return authService.usuario(clienteId);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestAttribute(AuthInterceptor.ATTR_TOKEN_HASH) String tokenHash) {
        authService.logout(tokenHash);
        return ResponseEntity.noContent().build();
    }
}
