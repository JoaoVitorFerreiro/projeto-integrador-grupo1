package com.projeto.banco.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "avaliacao")
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private Long pedidoId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    /** Primeiro nome + inicial do sobrenome, calculado na criação. */
    @Column(name = "nome_exibicao", nullable = false, length = 100)
    private String nomeExibicao;

    @Column(nullable = false)
    private Integer nota;

    @Column(length = 500)
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
