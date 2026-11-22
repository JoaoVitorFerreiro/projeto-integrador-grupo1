package com.projeto.banco.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;

/** Base comum de ClienteFisica e Clienteempresa (herança JOINED, tabela tb_cliente). */
@Entity
@Table(name = "tb_cliente")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String endereco;
    @Column(unique = true, length = 120)
    private String email;
    private String contato;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha; // DESCONTINUADO: nunca gravado em texto puro (ver senhaHash)

    /** Hash BCrypt da senha. Nunca serializado nem aceito via JSON. */
    @JsonIgnore
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;
}
