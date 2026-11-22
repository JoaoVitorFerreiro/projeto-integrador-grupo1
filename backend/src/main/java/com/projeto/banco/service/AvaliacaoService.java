package com.projeto.banco.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.banco.dto.ApiException;
import com.projeto.banco.dto.Dtos.AvaliacaoPublica;
import com.projeto.banco.dto.Dtos.AvaliacaoRequest;
import com.projeto.banco.dto.Dtos.AvaliacaoResponse;
import com.projeto.banco.model.Avaliacao;
import com.projeto.banco.model.Cliente;
import com.projeto.banco.model.Pedido;
import com.projeto.banco.repository.AvaliacaoRepository;
import com.projeto.banco.repository.ClienteRepository;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, ClienteRepository clienteRepository,
                            PedidoService pedidoService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoService = pedidoService;
    }

    @Transactional
    public AvaliacaoResponse criar(Long clienteId, AvaliacaoRequest r) {
        if (r == null || r.pedidoId() == null || r.pedidoId().isBlank()) throw ApiException.badRequest("pedidoId é obrigatório.");
        if (r.nota() == null || r.nota() < 1 || r.nota() > 5) throw ApiException.badRequest("A nota deve ser de 1 a 5.");
        String texto = r.texto() == null || r.texto().isBlank() ? null : r.texto().trim();
        if (texto != null && texto.length() > 500) throw ApiException.badRequest("O texto deve ter no máximo 500 caracteres.");
        Pedido p = pedidoService.obter(clienteId, r.pedidoId().trim()); // 404 se não for do cliente
        if (avaliacaoRepository.existsByPedidoId(p.getId())) throw ApiException.conflict("Este pedido já foi avaliado.");
        Cliente c = clienteRepository.findById(clienteId).orElseThrow(() -> ApiException.unauthorized("Sessão inválida."));
        Avaliacao a = new Avaliacao();
        a.setPedidoId(p.getId());
        a.setClienteId(clienteId);
        a.setNomeExibicao(nomeExibicao(c.getNome()));
        a.setNota(r.nota());
        a.setTexto(texto);
        a.setCriadoEm(Instant.now());
        a = avaliacaoRepository.saveAndFlush(a);
        return new AvaliacaoResponse(a.getId(), p.getPublicId(), a.getNota(), a.getTexto(), a.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoPublica> listarPublicas() {
        return avaliacaoRepository.findAllByOrderByCriadoEmDescIdDesc(PageRequest.of(0, 50)).stream()
                .map(a -> new AvaliacaoPublica(a.getNomeExibicao(), a.getNota(), a.getTexto(), a.getCriadoEm()))
                .toList();
    }

    /** "Maria da Silva" -> "Maria S." */
    static String nomeExibicao(String nome) {
        String[] partes = nome.trim().split("\\s+");
        if (partes.length < 2) return partes[0];
        return partes[0] + " " + Character.toUpperCase(partes[partes.length - 1].charAt(0)) + ".";
    }
}
