package com.projeto.banco.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.banco.model.CupomCliente;

public interface CupomClienteRepository extends JpaRepository<CupomCliente, Long> {
    List<CupomCliente> findByClienteId(Long clienteId);

    boolean existsByCupomIdAndClienteId(Long cupomId, Long clienteId);
}
