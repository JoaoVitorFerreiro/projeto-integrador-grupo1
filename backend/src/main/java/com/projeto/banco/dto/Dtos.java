package com.projeto.banco.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** DTOs (records) da API de contas, pedidos, avaliações e cupons. */
public final class Dtos {
    private Dtos() {}

    // ---- Auth ----
    public record RegisterRequest(String tipo, String nome, String email, String senha, String documento,
                                  String telefone, String endereco, String razaoSocial, String responsavel) {}

    public record LoginRequest(String email, String senha) {}

    public record UsuarioResponse(Long id, String nome, String email, String tipo, String documentoMascarado,
                                  String telefone, String endereco) {}

    public record AuthResponse(String token, UsuarioResponse usuario) {}

    // ---- Pedidos ----
    public record ItemRequest(String produtoId, String nome, String descricao, String imagem, BigDecimal preco,
                              Integer quantidade, String preparo, Map<String, Object> bolo) {}

    public record PedidoRequest(String filialId, String metodo, String data, String horario, String endereco,
                                String pagamento, String cupom, List<ItemRequest> itens) {}

    public record ItemResponse(Long id, String produtoId, String nome, String descricao, String imagem,
                               BigDecimal preco, Integer quantidade, String preparo, Map<String, Object> bolo) {}

    public record MensagemRequest(String texto) {}

    public record MensagemResponse(Long id, String autor, String texto, Instant enviadaEm) {}

    public record AvaliacaoPedidoResponse(Long id, Integer nota, String texto, Instant data) {}

    public record PedidoResponse(String id, String filialId, String filialNome, String metodo, String data,
                                 String horario, String endereco, String pagamento, String cupom,
                                 BigDecimal subtotal, BigDecimal taxaEntrega, BigDecimal desconto, BigDecimal total,
                                 String status, Instant criadoEm, List<ItemResponse> itens,
                                 List<MensagemResponse> mensagens, AvaliacaoPedidoResponse avaliacao) {}

    // ---- Avaliações ----
    public record AvaliacaoRequest(String pedidoId, Integer nota, String texto) {}

    public record AvaliacaoResponse(Long id, String pedidoId, Integer nota, String texto, Instant data) {}

    public record AvaliacaoPublica(String nome, Integer nota, String texto, Instant data) {}

    // ---- Cupons ----
    public record MeuCupom(String codigo, BigDecimal percentual, Boolean ativo, String descricao) {}
}
