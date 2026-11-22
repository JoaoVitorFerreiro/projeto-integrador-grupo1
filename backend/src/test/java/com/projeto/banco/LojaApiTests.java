package com.projeto.banco;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:lojatest;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LojaApiTests {

    @Autowired
    MockMvc mvc;

    private static final Random RND = new Random();

    // ---------- helpers ----------
    static String cpfValido() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) d[i] = RND.nextInt(10);
        d[0] = 1 + RND.nextInt(9);
        d[1] = (d[0] + 1) % 10; // evita todos iguais
        for (int n = 9; n <= 10; n++) {
            int s = 0;
            for (int i = 0; i < n; i++) s += d[i] * (n + 1 - i);
            int r = (s * 10) % 11;
            d[n] = r == 10 ? 0 : r;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    static String email() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    String registroJson(String tipo, String email, String doc) {
        return """
            {"tipo":"%s","nome":"Maria da Silva","email":"%s","senha":"senha-forte-1","documento":"%s",
             "telefone":"(11) 91234-5678","endereco":"Rua das Flores, 100"}""".formatted(tipo, email, doc);
    }

    String registrarPF(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PF", email, cpfValido())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    String pedidoJson(String metodo, String endereco, String pagamento, String cupom) {
        return """
            {"filialId":"pinheiros","metodo":"%s","data":"%s","horario":"23:30",%s"pagamento":"%s",%s
             "itens":[{"produtoId":"p1","nome":"Bolo","descricao":"d","imagem":"i.webp","preco":50.00,"quantidade":2,
                       "preparo":null,"bolo":{"andares":2,"sabor":"chocolate"}},
                      {"produtoId":"p2","nome":"Coxinha","descricao":"d","imagem":"c.webp","preco":2.50,"quantidade":10,"preparo":"fried"}]}"""
                .formatted(metodo, LocalDate.now().plusDays(1), endereco == null ? "" : "\"endereco\":\"" + endereco + "\",",
                        pagamento, cupom == null ? "" : "\"cupom\":\"" + cupom + "\",");
    }

    // ---------- registro ----------
    @Test
    void registroValidoNaoVazaSegredos() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PF", email(), cpfValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.tipo").value("PF"))
                .andExpect(jsonPath("$.usuario.documentoMascarado").value(org.hamcrest.Matchers.matchesPattern("\\*\\*\\*\\.\\d{3}\\.\\d{3}-\\*\\*")))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("senha-forte-1"));
    }

    @Test
    void registroPJ() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PJ", email(), "11.444.777/0001-61")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.tipo").value("PJ"))
                .andExpect(jsonPath("$.usuario.documentoMascarado").value("**.444.777/0001-**"));
    }

    @Test
    void registroCpfInvalido() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PF", email(), "123.456.789-00")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("CPF inválido."));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PJ", email(), "11.111.111/1111-11")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroSenhaCurtaEJsonInvalido() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PF", email(), cpfValido()).replace("senha-forte-1", "curta")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{nao-json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void registroEmailOuDocumentoDuplicado() throws Exception {
        String e = email();
        String cpf = cpfValido();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registroJson("PF", e, cpf)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PF", e.toUpperCase(), cpfValido())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registroJson("PF", email(), cpf)))
                .andExpect(status().isConflict());
    }

    // ---------- login / me / logout ----------
    @Test
    void loginOkFalhoMeLogout() throws Exception {
        String e = email();
        registrarPF(e);
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e + "\",\"senha\":\"senha-forte-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.email").value(e))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(e))
                .andExpect(jsonPath("$.token").doesNotExist());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e + "\",\"senha\":\"errada-errada\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.erro").value("E-mail ou senha incorretos."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"naoexiste@x.com\",\"senha\":\"errada-errada\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.erro").value("E-mail ou senha incorretos."));

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginLimiteDeTentativas() throws Exception {
        String e = email();
        registrarPF(e);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + e + "\",\"senha\":\"errada-errada\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e + "\",\"senha\":\"senha-forte-1\"}")).andExpect(status().isTooManyRequests());
    }

    @Test
    void rotasProtegidasSemTokenRetornam401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.erro").exists());
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/meus-cupons")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer lixo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/avaliacoes")).andExpect(status().isOk());
        mvc.perform(get("/api/bolos/modelos")).andExpect(status().isOk()).andExpect(jsonPath("$.modelos").isArray());
    }

    // ---------- cupons / pedidos ----------
    @Test
    void pedidoComCupomDoClienteCalculaNoServidor() throws Exception {
        String token = registrarPF(email());
        mvc.perform(get("/api/meus-cupons").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").value("BEMVINDO10"))
                .andExpect(jsonPath("$[0].percentual").value(10)).andExpect(jsonPath("$[0].ativo").value(true));

        // subtotal = 2*50 + 10*2.50 = 125.00 ; desconto 10% = 12.50 ; entrega 12 ; total 124.50
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("delivery", "Rua A, 123", "Pix", "bemvindo10")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("M\\d{6}")))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.subtotal").value(125.0))
                .andExpect(jsonPath("$.desconto").value(12.5))
                .andExpect(jsonPath("$.taxaEntrega").value(12.0))
                .andExpect(jsonPath("$.total").value(124.5))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].bolo.sabor").value("chocolate"))
                .andExpect(jsonPath("$.mensagens[0].autor").value("LOJA"))
                .andExpect(jsonPath("$.mensagens[0].texto").value(org.hamcrest.Matchers.containsString("Maison Pinheiros")));
    }

    @Test
    void cupomDeOutroClienteOuInativoOuInexistenteRetorna400() throws Exception {
        String pf = registrarPF(email());
        String pjBody = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("PJ", email(), "11.222.333/0001-81"))).andReturn().getResponse().getContentAsString();
        // CNPJ pode ja existir por outro teste: so seguimos se criou
        // EMPRESA15 pertence apenas a PJ
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + pf).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("pickup", null, "Pix", "EMPRESA15"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + pf).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("pickup", null, "Pix", "NATAL20"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + pf).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("pickup", null, "Pix", "XXXX"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
        org.junit.jupiter.api.Assertions.assertNotNull(pjBody);
    }

    @Test
    void deliverySemEnderecoEPagarNaRetiradaComDelivery() throws Exception {
        String token = registrarPF(email());
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("delivery", null, "Pix", null))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("delivery", "Rua A, 123", "Pagar na retirada", null))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("pickup", null, "Pagar na retirada", null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.taxaEntrega").value(0))
                .andExpect(jsonPath("$.total").value(125.0));
    }

    @Test
    void chatEIsolamentoEntreClientes() throws Exception {
        String a = registrarPF(email());
        String b = registrarPF(email());
        String body = mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson("pickup", null, "Pix", null))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(post("/api/pedidos/" + id + "/mensagens").header("Authorization", "Bearer " + a)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Posso retirar mais cedo?\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.autor").value("CLIENTE"))
                .andExpect(jsonPath("$.enviadaEm").exists());
        mvc.perform(post("/api/pedidos/" + id + "/mensagens").header("Authorization", "Bearer " + a)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/pedidos/" + id + "/mensagens").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].autor").value("LOJA")).andExpect(jsonPath("$[1].autor").value("CLIENTE"));
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + a)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mensagens.length()").value(2));

        // outro cliente nao enxerga
        mvc.perform(get("/api/pedidos/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
        mvc.perform(post("/api/pedidos/" + id + "/mensagens").header("Authorization", "Bearer " + b)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"oi\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + b)).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- avaliacoes ----------
    @Test
    void avaliacaoUnicaPorPedido() throws Exception {
        String a = registrarPF(email());
        String b = registrarPF(email());
        String id = JsonPath.read(mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + a)
                .contentType(MediaType.APPLICATION_JSON).content(pedidoJson("pickup", null, "Pix", null)))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/avaliacoes").header("Authorization", "Bearer " + b).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":\"" + id + "\",\"nota\":5}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/avaliacoes").header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":\"" + id + "\",\"nota\":6}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/avaliacoes").header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":\"" + id + "\",\"nota\":5,\"texto\":\"Delicioso\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nota").value(5));
        mvc.perform(post("/api/avaliacoes").header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":\"" + id + "\",\"nota\":4}")).andExpect(status().isConflict());

        mvc.perform(get("/api/avaliacoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria S.")).andExpect(jsonPath("$[0].texto").value("Delicioso"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get("/api/pedidos/" + id).header("Authorization", "Bearer " + a))
                .andExpect(jsonPath("$.avaliacao.nota").value(5));
    }
}
