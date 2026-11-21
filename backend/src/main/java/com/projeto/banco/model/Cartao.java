package com.projeto.banco.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_cartao")
@Getter
@Setter
@NoArgsConstructor
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipoCartao;

    @Column(nullable = false)
    private String valor;

    @Column(nullable = false)
    private String nome;

    /** O CVC nao pode ser armazenado (PCI): aceito na entrada, nunca persistido nem devolvido. */
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cvc;

    @Column(nullable = false)
    private String validade;

    @Column(nullable = false)
    private String numeroCartao;

    public Cartao(String tipoCartao, String valor, String nome, String cvc, String validade, String numeroCartao) {
        this.tipoCartao = tipoCartao;
        this.valor = valor;
        this.nome = nome;
        this.cvc = cvc;
        this.validade = validade;
        this.numeroCartao = numeroCartao;
    }
}
