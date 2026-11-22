package com.projeto.banco.security;

import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.projeto.banco.dto.ApiException;
import com.projeto.banco.model.Sessao;
import com.projeto.banco.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Exige "Authorization: Bearer <token>" nas rotas protegidas (ver WebConfig). */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_CLIENTE_ID = "auth.clienteId";
    public static final String ATTR_TOKEN_HASH = "auth.tokenHash";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String method = req.getMethod();
        if (HttpMethod.OPTIONS.matches(method)) return true;
        String path = req.getRequestURI();
        // GET /api/avaliacoes é público; POST /api/clientes (cadastro legado) é público
        if (HttpMethod.GET.matches(method) && path.equals("/api/avaliacoes")) return true;
        if (HttpMethod.POST.matches(method) && path.equals("/api/clientes")) return true;

        String header = req.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw ApiException.unauthorized("Autenticação necessária.");
        }
        String token = header.substring(7).trim();
        Sessao s = authService.validarToken(token)
                .orElseThrow(() -> ApiException.unauthorized("Sessão inválida ou expirada."));
        req.setAttribute(ATTR_CLIENTE_ID, s.getClienteId());
        req.setAttribute(ATTR_TOKEN_HASH, s.getTokenHash());
        return true;
    }
}
