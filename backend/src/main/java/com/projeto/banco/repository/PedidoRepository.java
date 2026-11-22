package com.projeto.banco.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.banco.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteIdOrderByCriadoEmDescIdDesc(Long clienteId);

    Optional<Pedido> findByPublicIdAndClienteId(String publicId, Long clienteId);

    boolean existsByPublicId(String publicId);
}
