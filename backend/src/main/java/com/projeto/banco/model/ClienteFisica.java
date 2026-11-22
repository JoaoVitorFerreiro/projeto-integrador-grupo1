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
@Table(name = "tb_cliente_fisica")
@PrimaryKeyJoinColumn(name = "cliente_id") // Liga o registro de PF ao ID da tabela tb_cliente
@Getter
@Setter
@NoArgsConstructor
public class ClienteFisica extends Cliente {

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public ClienteFisica(String nome, String cpf, String endereco, String email, String contato, String senha) {
        setNome(nome);
        setEndereco(endereco);
        setEmail(email);
        setContato(contato);
        setSenha(senha);
        this.cpf = cpf;
    }
}
