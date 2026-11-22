package com.projeto.banco.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.banco.model.Avaliacao;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Optional<Avaliacao> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);

    List<Avaliacao> findAllByOrderByCriadoEmDescIdDesc(Pageable pageable);
}
