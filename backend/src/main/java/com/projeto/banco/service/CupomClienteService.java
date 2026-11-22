package com.projeto.banco.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.banco.dto.Dtos.MeuCupom;
import com.projeto.banco.model.Cupom;
import com.projeto.banco.model.CupomCliente;
import com.projeto.banco.model.TipoDescontoCupom;
import com.projeto.banco.repository.CupomClienteRepository;
import com.projeto.banco.repository.CupomRepository;

@Service
public class CupomClienteService {

    public static final String CUPOM_PF = "BEMVINDO10";
    public static final String CUPOM_PJ = "EMPRESA15";

    private final CupomRepository cupomRepository;
    private final CupomClienteRepository cupomClienteRepository;

    public CupomClienteService(CupomRepository cupomRepository, CupomClienteRepository cupomClienteRepository) {
        this.cupomRepository = cupomRepository;
        this.cupomClienteRepository = cupomClienteRepository;
    }

    /** Idempotente: cria os cupons seed que ainda não existem. */
    @Transactional
    public void garantirSeeds() {
        seed(CUPOM_PF, "10% de boas-vindas", 10, true);
        seed(CUPOM_PJ, "15% para empresas", 15, true);
        seed("NATAL20", "20% de Natal", 20, false);
    }

    private void seed(String codigo, String descricao, int percentual, boolean ativo) {
        if (cupomRepository.existsByCodigoIgnoreCase(codigo)) return;
        LocalDateTime agora = LocalDateTime.now();
        Cupom c = new Cupom(codigo, descricao, TipoDescontoCupom.PERCENTUAL, BigDecimal.valueOf(percentual),
                BigDecimal.ZERO, agora.minusDays(1), agora.plusYears(10), 1_000_000);
        c.setAtivo(ativo);
        cupomRepository.save(c);
    }

    @Transactional
    public void concederCupom(String codigo, Long clienteId) {
        garantirSeeds();
        Cupom c = cupomRepository.findByCodigoIgnoreCase(codigo).orElseThrow();
        if (!cupomClienteRepository.existsByCupomIdAndClienteId(c.getId(), clienteId)) {
            cupomClienteRepository.save(new CupomCliente(c.getId(), clienteId));
        }
    }

    @Transactional(readOnly = true)
    public List<MeuCupom> listarDoCliente(Long clienteId) {
        LocalDateTime agora = LocalDateTime.now();
        return cupomClienteRepository.findByClienteId(clienteId).stream()
                .map(cc -> cupomRepository.findById(cc.getCupomId()).orElse(null))
                .filter(c -> c != null)
                .map(c -> new MeuCupom(c.getCodigo(), percentualDe(c),
                        estaVigente(c, agora), c.getDescricao()))
                .toList();
    }

    public static boolean estaVigente(Cupom c, LocalDateTime agora) {
        return Boolean.TRUE.equals(c.getAtivo())
                && !agora.isBefore(c.getDataInicio()) && !agora.isAfter(c.getDataValidade());
    }

    /** Para cupons de valor fixo devolve 0 (o contrato do front só conhece percentual). */
    private static BigDecimal percentualDe(Cupom c) {
        if (c.getTipoDesconto() != TipoDescontoCupom.PERCENTUAL) return BigDecimal.ZERO;
        BigDecimal v = c.getValorDesconto().stripTrailingZeros();
        return v.scale() < 0 ? v.setScale(0) : v;
    }
}
