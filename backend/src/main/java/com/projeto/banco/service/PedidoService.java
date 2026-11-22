package com.projeto.banco.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.banco.dto.ApiException;
import com.projeto.banco.dto.Dtos.AvaliacaoPedidoResponse;
import com.projeto.banco.dto.Dtos.ItemRequest;
import com.projeto.banco.dto.Dtos.ItemResponse;
import com.projeto.banco.dto.Dtos.MensagemResponse;
import com.projeto.banco.dto.Dtos.PedidoRequest;
import com.projeto.banco.dto.Dtos.PedidoResponse;
import com.projeto.banco.model.Cupom;
import com.projeto.banco.model.Pedido;
import com.projeto.banco.model.PedidoItem;
import com.projeto.banco.model.PedidoMensagem;
import com.projeto.banco.model.PedidoMensagem.Autor;
import com.projeto.banco.model.StatusPedido;
import com.projeto.banco.model.TipoDescontoCupom;
import com.projeto.banco.repository.AvaliacaoRepository;
import com.projeto.banco.repository.CupomClienteRepository;
import com.projeto.banco.repository.CupomRepository;
import com.projeto.banco.repository.PedidoMensagemRepository;
import com.projeto.banco.repository.PedidoRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PedidoService {

    static final BigDecimal TAXA_ENTREGA = new BigDecimal("12.00");
    private static final Map<String, String> FILIAIS = Map.of(
            "jardins", "Jardins", "pinheiros", "Pinheiros", "moema", "Moema");
    private static final Set<String> PAGAMENTOS = Set.of("Pix", "Cartão de crédito", "Cartão de débito",
            "Pagar na retirada");
    private static final Pattern HORARIO = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final BigDecimal CEM = new BigDecimal("100");

    private final SecureRandom random = new SecureRandom();
    private final PedidoRepository pedidoRepository;
    private final PedidoMensagemRepository mensagemRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CupomRepository cupomRepository;
    private final CupomClienteRepository cupomClienteRepository;
    private final JsonMapper jsonMapper;

    public PedidoService(PedidoRepository pedidoRepository, PedidoMensagemRepository mensagemRepository,
                         AvaliacaoRepository avaliacaoRepository, CupomRepository cupomRepository,
                         CupomClienteRepository cupomClienteRepository, JsonMapper jsonMapper) {
        this.pedidoRepository = pedidoRepository;
        this.mensagemRepository = mensagemRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.cupomRepository = cupomRepository;
        this.cupomClienteRepository = cupomClienteRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public PedidoResponse criar(Long clienteId, PedidoRequest r) {
        if (r == null) throw ApiException.badRequest("Corpo da requisição inválido.");
        String filial = r.filialId() == null ? "" : r.filialId().trim().toLowerCase();
        if (!FILIAIS.containsKey(filial)) throw ApiException.badRequest("Filial inválida (jardins, pinheiros ou moema).");
        String metodo = r.metodo() == null ? "" : r.metodo().trim();
        if (!metodo.equals("pickup") && !metodo.equals("delivery")) {
            throw ApiException.badRequest("Método deve ser pickup ou delivery.");
        }
        boolean delivery = metodo.equals("delivery");
        LocalDate data;
        try {
            data = LocalDate.parse(r.data() == null ? "" : r.data().trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Data inválida (use YYYY-MM-DD).");
        }
        if (data.isBefore(LocalDate.now())) throw ApiException.badRequest("A data não pode estar no passado.");
        String horario = r.horario() == null ? "" : r.horario().trim();
        if (!HORARIO.matcher(horario).matches()) throw ApiException.badRequest("Horário inválido (use HH:MM).");
        if (data.equals(LocalDate.now()) && LocalTime.parse(horario).isBefore(LocalTime.now())) {
            throw ApiException.badRequest("O horário não pode estar no passado.");
        }
        String endereco = r.endereco() == null ? null : r.endereco().trim();
        if (delivery && (endereco == null || endereco.length() < 8)) {
            throw ApiException.badRequest("Endereço é obrigatório para entrega.");
        }
        if (endereco != null && endereco.length() > 300) throw ApiException.badRequest("Endereço muito longo.");
        if (!delivery) endereco = null;
        String pagamento = r.pagamento() == null ? "" : r.pagamento().trim();
        if (!PAGAMENTOS.contains(pagamento)) throw ApiException.badRequest("Forma de pagamento inválida.");
        if (pagamento.equals("Pagar na retirada") && delivery) {
            throw ApiException.badRequest("\"Pagar na retirada\" só está disponível para retirada.");
        }

        if (r.itens() == null || r.itens().isEmpty()) throw ApiException.badRequest("O pedido precisa ter ao menos um item.");
        if (r.itens().size() > 50) throw ApiException.badRequest("Itens demais no pedido (máx. 50).");
        Pedido p = new Pedido();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest i : r.itens()) {
            PedidoItem item = converterItem(i);
            subtotal = subtotal.add(item.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())));
            p.getItens().add(item);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal desconto = BigDecimal.ZERO.setScale(2);
        String codigoCupom = null;
        if (r.cupom() != null && !r.cupom().isBlank()) {
            Cupom c = cupomRepository.findByCodigoIgnoreCase(r.cupom().trim()).orElse(null);
            LocalDateTime agora = LocalDateTime.now();
            if (c == null || !cupomClienteRepository.existsByCupomIdAndClienteId(c.getId(), clienteId)
                    || !CupomClienteService.estaVigente(c, agora)
                    || subtotal.compareTo(c.getValorMinimoPedido()) < 0) {
                throw ApiException.badRequest("Cupom inválido ou indisponível para esta conta.");
            }
            desconto = c.getTipoDesconto() == TipoDescontoCupom.PERCENTUAL
                    ? subtotal.multiply(c.getValorDesconto()).divide(CEM, 2, RoundingMode.HALF_UP)
                    : c.getValorDesconto().min(subtotal).setScale(2, RoundingMode.HALF_UP);
            codigoCupom = c.getCodigo();
        }
        BigDecimal taxa = delivery ? TAXA_ENTREGA : BigDecimal.ZERO.setScale(2);

        p.setPublicId(gerarPublicId());
        p.setClienteId(clienteId);
        p.setFilialId(filial);
        p.setMetodo(metodo);
        p.setDataAgendada(data);
        p.setHorario(horario);
        p.setEndereco(endereco);
        p.setPagamento(pagamento);
        p.setCupom(codigoCupom);
        p.setSubtotal(subtotal);
        p.setTaxaEntrega(taxa);
        p.setDesconto(desconto);
        p.setTotal(subtotal.subtract(desconto).add(taxa));
        p.setStatus(StatusPedido.SCHEDULED);
        p.setCriadoEm(Instant.now());
        p = pedidoRepository.save(p);

        mensagemRepository.save(new PedidoMensagem(p.getId(), Autor.LOJA,
                "Olá! Recebemos seu pedido. Deixe sua mensagem por aqui que a Maison " + FILIAIS.get(filial)
                        + " responde assim que possível."));
        return paraResposta(p);
    }

    private PedidoItem converterItem(ItemRequest i) {
        if (i == null) throw ApiException.badRequest("Item inválido.");
        PedidoItem item = new PedidoItem();
        item.setProdutoId(limitar(i.produtoId(), 60, "produtoId"));
        String nome = limitar(i.nome(), 200, "nome do item");
        if (nome == null || nome.isBlank()) throw ApiException.badRequest("Nome do item é obrigatório.");
        item.setNome(nome);
        item.setDescricao(limitar(i.descricao(), 500, "descrição do item"));
        item.setImagem(limitar(i.imagem(), 500, "imagem do item"));
        if (i.preco() == null || i.preco().signum() <= 0 || i.preco().compareTo(new BigDecimal("100000")) > 0) {
            throw ApiException.badRequest("Preço do item inválido.");
        }
        item.setPreco(i.preco().setScale(2, RoundingMode.HALF_UP));
        if (item.getPreco().signum() <= 0) throw ApiException.badRequest("Preço do item inválido.");
        if (i.quantidade() == null || i.quantidade() < 1 || i.quantidade() > 500) {
            throw ApiException.badRequest("Quantidade deve ser de 1 a 500.");
        }
        item.setQuantidade(i.quantidade());
        if (i.preparo() != null && !i.preparo().equals("fried") && !i.preparo().equals("frozen")) {
            throw ApiException.badRequest("Preparo deve ser fried, frozen ou nulo.");
        }
        item.setPreparo(i.preparo());
        if (i.bolo() != null) {
            String json = jsonMapper.writeValueAsString(i.bolo());
            if (json.length() > 4000) throw ApiException.badRequest("Configuração do bolo muito grande.");
            item.setBoloJson(json);
        }
        return item;
    }

    private static String limitar(String s, int max, String campo) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() > max) throw ApiException.badRequest("Campo " + campo + " excede " + max + " caracteres.");
        return s;
    }

    private String gerarPublicId() {
        for (int t = 0; t < 50; t++) {
            String id = "M" + (100000 + random.nextInt(900000));
            if (!pedidoRepository.existsByPublicId(id)) return id;
        }
        throw new IllegalStateException("Não foi possível gerar o identificador do pedido.");
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId) {
        return pedidoRepository.findByClienteIdOrderByCriadoEmDescIdDesc(clienteId).stream()
                .map(this::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long clienteId, String publicId) {
        return paraResposta(obter(clienteId, publicId));
    }

    @Transactional(readOnly = true)
    public Pedido obter(Long clienteId, String publicId) {
        return pedidoRepository.findByPublicIdAndClienteId(publicId, clienteId)
                .orElseThrow(() -> ApiException.notFound("Pedido não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> mensagens(Long clienteId, String publicId) {
        return mensagensDe(obter(clienteId, publicId).getId());
    }

    @Transactional
    public MensagemResponse enviarMensagem(Long clienteId, String publicId, String texto) {
        Pedido p = obter(clienteId, publicId);
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty() || t.length() > 500) throw ApiException.badRequest("A mensagem deve ter de 1 a 500 caracteres.");
        return paraMensagem(mensagemRepository.save(new PedidoMensagem(p.getId(), Autor.CLIENTE, t)));
    }

    private List<MensagemResponse> mensagensDe(Long pedidoId) {
        return mensagemRepository.findByPedidoIdOrderByEnviadaEmAscIdAsc(pedidoId).stream()
                .map(this::paraMensagem).toList();
    }

    private MensagemResponse paraMensagem(PedidoMensagem m) {
        return new MensagemResponse(m.getId(), m.getAutor().name(), m.getTexto(), m.getEnviadaEm());
    }

    private PedidoResponse paraResposta(Pedido p) {
        List<ItemResponse> itens = p.getItens().stream().map(i -> new ItemResponse(i.getId(), i.getProdutoId(),
                i.getNome(), i.getDescricao(), i.getImagem(), i.getPreco(), i.getQuantidade(), i.getPreparo(),
                i.getBoloJson() == null ? null
                        : jsonMapper.readValue(i.getBoloJson(), new TypeReference<Map<String, Object>>() {})))
                .toList();
        AvaliacaoPedidoResponse av = avaliacaoRepository.findByPedidoId(p.getId())
                .map(a -> new AvaliacaoPedidoResponse(a.getId(), a.getNota(), a.getTexto(), a.getCriadoEm()))
                .orElse(null);
        return new PedidoResponse(p.getPublicId(), p.getFilialId(), "Maison " + FILIAIS.get(p.getFilialId()),
                p.getMetodo(), p.getDataAgendada().toString(), p.getHorario(), p.getEndereco(), p.getPagamento(),
                p.getCupom(), p.getSubtotal(), p.getTaxaEntrega(), p.getDesconto(), p.getTotal(),
                p.getStatus().name(), p.getCriadoEm(), itens, mensagensDe(p.getId()), av);
    }
}
