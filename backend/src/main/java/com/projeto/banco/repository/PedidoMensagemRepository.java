package com.projeto.banco.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.banco.model.PedidoMensagem;

public interface PedidoMensagemRepository extends JpaRepository<PedidoMensagem, Long> {
    List<PedidoMensagem> findByPedidoIdOrderByEnviadaEmAscIdAsc(Long pedidoId);
}
