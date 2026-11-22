package com.projeto.banco.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pedido da loja online (independente de Encomenda, que tem outro modelo de negócio). */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 10)
    private String publicId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "filial_id", nullable = false, length = 20)
    private String filialId;

    @Column(nullable = false, length = 10)
    private String metodo;

    @Column(name = "data_agendada", nullable = false)
    private LocalDate dataAgendada;

    @Column(nullable = false, length = 5)
    private String horario;

    @Column(length = 300)
    private String endereco;

    @Column(nullable = false, length = 30)
    private String pagamento;

    @Column(length = 30)
    private String cupom;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "taxa_entrega", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaEntrega;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusPedido status = StatusPedido.SCHEDULED;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id")
    @OrderBy("id ASC")
    private List<PedidoItem> itens = new ArrayList<>();
}
