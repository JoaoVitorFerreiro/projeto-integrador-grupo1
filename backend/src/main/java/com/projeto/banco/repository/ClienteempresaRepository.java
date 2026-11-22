package com.projeto.banco.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.banco.model.Clienteempresa;

public interface ClienteempresaRepository extends JpaRepository<Clienteempresa, Long> {
    boolean existsByCnpj(String cnpj);
}
