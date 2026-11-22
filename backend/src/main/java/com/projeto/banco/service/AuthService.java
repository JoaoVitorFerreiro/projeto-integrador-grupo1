package com.projeto.banco.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.banco.dto.ApiException;
import com.projeto.banco.dto.Dtos.AuthResponse;
import com.projeto.banco.dto.Dtos.LoginRequest;
import com.projeto.banco.dto.Dtos.RegisterRequest;
import com.projeto.banco.dto.Dtos.UsuarioResponse;
import com.projeto.banco.model.Cliente;
import com.projeto.banco.model.ClienteFisica;
import com.projeto.banco.model.Clienteempresa;
import com.projeto.banco.model.Sessao;
import com.projeto.banco.repository.ClienteFisicaRepository;
import com.projeto.banco.repository.ClienteRepository;
import com.projeto.banco.repository.ClienteempresaRepository;
import com.projeto.banco.repository.SessaoRepository;
import com.projeto.banco.security.Documentos;

@Service
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final String MSG_LOGIN = "E-mail ou senha incorretos.";
    private static final Duration VALIDADE = Duration.ofDays(7);
    private static final Duration JANELA = Duration.ofMinutes(10);
    private static final int MAX_FALHAS = 5;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final String hashFalso = encoder.encode("senha-inexistente");
    private final ConcurrentHashMap<String, java.util.ArrayDeque<Instant>> falhas = new ConcurrentHashMap<>();

    private final ClienteRepository clienteRepository;
    private final ClienteFisicaRepository fisicaRepository;
    private final ClienteempresaRepository empresaRepository;
    private final SessaoRepository sessaoRepository;
    private final CupomClienteService cupomClienteService;

    public AuthService(ClienteRepository clienteRepository, ClienteFisicaRepository fisicaRepository,
                       ClienteempresaRepository empresaRepository, SessaoRepository sessaoRepository,
                       CupomClienteService cupomClienteService) {
        this.clienteRepository = clienteRepository;
        this.fisicaRepository = fisicaRepository;
        this.empresaRepository = empresaRepository;
        this.sessaoRepository = sessaoRepository;
        this.cupomClienteService = cupomClienteService;
    }

    public String hashSenha(String senha) {
        return encoder.encode(senha);
    }

    // ---------------- registro ----------------
    @Transactional
    public AuthResponse registrar(RegisterRequest r) {
        if (r == null) throw ApiException.badRequest("Corpo da requisição inválido.");
        String tipo = r.tipo() == null ? "" : r.tipo().trim().toUpperCase();
        if (!tipo.equals("PF") && !tipo.equals("PJ")) throw ApiException.badRequest("Tipo deve ser PF ou PJ.");
        String nome = r.nome() == null ? "" : r.nome().trim();
        if (nome.length() < 2 || nome.length() > 100) throw ApiException.badRequest("Nome deve ter de 2 a 100 caracteres.");
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        if (email.length() > 120 || !EMAIL.matcher(email).matches()) throw ApiException.badRequest("E-mail inválido.");
        String senha = r.senha();
        if (senha == null || senha.length() < 8 || senha.length() > 72
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.badRequest("A senha deve ter de 8 a 72 caracteres.");
        }
        String doc = Documentos.somenteDigitos(r.documento());
        if (tipo.equals("PF") && !Documentos.cpfValido(doc)) throw ApiException.badRequest("CPF inválido.");
        if (tipo.equals("PJ") && !Documentos.cnpjValido(doc)) throw ApiException.badRequest("CNPJ inválido.");
        String tel = Documentos.somenteDigitos(r.telefone());
        if (tel.length() < 10 || tel.length() > 13) throw ApiException.badRequest("Telefone deve ter de 10 a 13 dígitos.");
        String endereco = r.endereco() == null ? "" : r.endereco().trim();
        if (endereco.length() < 8 || endereco.length() > 255) {
            throw ApiException.badRequest("Endereço deve ter de 8 a 255 caracteres.");
        }

        if (clienteRepository.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Já existe uma conta com este e-mail.");
        boolean docExiste = tipo.equals("PF") ? fisicaRepository.existsByCpf(doc) : empresaRepository.existsByCnpj(doc);
        if (docExiste) throw ApiException.conflict("Já existe uma conta com este documento.");

        Cliente salvo;
        if (tipo.equals("PF")) {
            ClienteFisica pf = new ClienteFisica();
            pf.setCpf(doc);
            salvo = pf;
        } else {
            Clienteempresa pj = new Clienteempresa();
            pj.setCnpj(doc);
            String razao = r.razaoSocial() == null || r.razaoSocial().isBlank() ? nome : r.razaoSocial().trim();
            if (razao.length() > 255) throw ApiException.badRequest("Razão social muito longa.");
            pj.setRazaoSocial(razao);
            if (r.responsavel() != null && !r.responsavel().isBlank()) {
                if (r.responsavel().trim().length() > 255) throw ApiException.badRequest("Responsável muito longo.");
                pj.setRepresentanteLegal(r.responsavel().trim());
            }
            salvo = pj;
        }
        salvo.setNome(nome);
        salvo.setEmail(email);
        salvo.setContato(tel);
        salvo.setEndereco(endereco);
        salvo.setSenhaHash(encoder.encode(senha));
        salvo = clienteRepository.saveAndFlush(salvo);

        cupomClienteService.concederCupom(tipo.equals("PF") ? CupomClienteService.CUPOM_PF
                : CupomClienteService.CUPOM_PJ, salvo.getId());

        return new AuthResponse(criarSessao(salvo.getId()), paraUsuario(salvo));
    }

    // ---------------- login ----------------
    @Transactional
    public AuthResponse login(LoginRequest r) {
        if (r == null || r.email() == null || r.senha() == null) throw ApiException.unauthorized(MSG_LOGIN);
        String email = r.email().trim().toLowerCase();
        verificarLimite(email);
        Cliente c = clienteRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = c != null && c.getSenhaHash() != null ? c.getSenhaHash() : hashFalso;
        boolean ok = encoder.matches(r.senha(), hash) && c != null && c.getSenhaHash() != null;
        if (!ok) {
            registrarFalha(email);
            throw ApiException.unauthorized(MSG_LOGIN);
        }
        falhas.remove(email);
        return new AuthResponse(criarSessao(c.getId()), paraUsuario(c));
    }

    private void verificarLimite(String email) {
        var fila = falhas.get(email);
        if (fila == null) return;
        synchronized (fila) {
            podar(fila);
            if (fila.size() >= MAX_FALHAS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas de login. Tente novamente em alguns minutos.");
            }
        }
    }

    private void registrarFalha(String email) {
        if (falhas.size() > 10_000) falhas.values().removeIf(f -> { synchronized (f) { podar(f); return f.isEmpty(); } });
        var fila = falhas.computeIfAbsent(email, k -> new java.util.ArrayDeque<>());
        synchronized (fila) {
            podar(fila);
            fila.addLast(Instant.now());
        }
    }

    private void podar(java.util.ArrayDeque<Instant> fila) {
        Instant limite = Instant.now().minus(JANELA);
        while (!fila.isEmpty() && fila.peekFirst().isBefore(limite)) fila.pollFirst();
    }

    // ---------------- sessão ----------------
    private String criarSessao(Long clienteId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sessao s = new Sessao();
        s.setTokenHash(sha256(token));
        s.setClienteId(clienteId);
        s.setCriadoEm(Instant.now());
        s.setExpiraEm(Instant.now().plus(VALIDADE));
        sessaoRepository.save(s);
        return token;
    }

    @Transactional
    public Optional<Sessao> validarToken(String token) {
        if (token == null || token.isEmpty() || token.length() > 200) return Optional.empty();
        Optional<Sessao> s = sessaoRepository.findByTokenHash(sha256(token));
        if (s.isPresent() && s.get().getExpiraEm().isBefore(Instant.now())) {
            sessaoRepository.delete(s.get());
            return Optional.empty();
        }
        return s;
    }

    @Transactional
    public void logout(String tokenHash) {
        sessaoRepository.deleteByTokenHash(tokenHash);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuario(Long clienteId) {
        return paraUsuario(clienteRepository.findById(clienteId)
                .orElseThrow(() -> ApiException.unauthorized("Sessão inválida ou expirada.")));
    }

    public UsuarioResponse paraUsuario(Cliente c) {
        String tipo;
        String doc;
        if (c instanceof ClienteFisica pf) {
            tipo = "PF";
            doc = Documentos.mascararCpf(pf.getCpf());
        } else if (c instanceof Clienteempresa pj) {
            tipo = "PJ";
            doc = Documentos.mascararCnpj(pj.getCnpj());
        } else {
            tipo = null;
            doc = null;
        }
        return new UsuarioResponse(c.getId(), c.getNome(), c.getEmail(), tipo, doc, c.getContato(), c.getEndereco());
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
