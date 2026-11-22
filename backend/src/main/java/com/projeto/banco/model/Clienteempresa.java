package com.projeto.banco.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_cliente_empresa")
@PrimaryKeyJoinColumn(name = "cliente_id") // Chave estrangeira que liga à tabela tb_cliente
@Getter
@Setter
@NoArgsConstructor
public class Clienteempresa extends Cliente {

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(nullable = false)
    private String razaoSocial;

    private String inscricaoEstadual;
    private String representanteLegal;

    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public Clienteempresa(String nome, String razaoSocial, String cnpj, String inscricaoEstadual,
                          String representanteLegal, String endereco, String email, String contato, String senha) {
        setNome(nome);
        setEndereco(endereco);
        setEmail(email);
        setContato(contato);
        setSenha(senha);
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.inscricaoEstadual = inscricaoEstadual;
        this.representanteLegal = representanteLegal;
    }
}
