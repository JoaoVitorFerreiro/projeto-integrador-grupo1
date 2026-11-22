package com.projeto.banco.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido_mensagem")
@Getter
@Setter
@NoArgsConstructor
public class PedidoMensagem {

    public enum Autor { CLIENTE, LOJA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Autor autor;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;

    public PedidoMensagem(Long pedidoId, Autor autor, String texto) {
        this.pedidoId = pedidoId;
        this.autor = autor;
        this.texto = texto;
        this.enviadaEm = Instant.now();
    }
}
