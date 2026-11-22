package com.projeto.banco.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Associação cupom x cliente (quais cupons pertencem a quem). */
@Entity
@Table(name = "cupom_cliente", uniqueConstraints = @UniqueConstraint(columnNames = { "cupom_id", "cliente_id" }))
@Getter
@Setter
@NoArgsConstructor
public class CupomCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cupom_id", nullable = false)
    private Long cupomId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    public CupomCliente(Long cupomId, Long clienteId) {
        this.cupomId = cupomId;
        this.clienteId = clienteId;
    }
}
