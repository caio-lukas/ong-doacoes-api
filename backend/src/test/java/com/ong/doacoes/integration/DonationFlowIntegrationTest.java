package com.ong.doacoes.integration;

import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.entity.Item;
import com.ong.doacoes.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa, com um banco real, as regras que unem doações, estoque e histórico. */
class DonationFlowIntegrationTest extends IntegrationTestBase {

    private long registrarDoacaoDeItens(String token, Donor doador, Item item, int quantidade) throws Exception {
        String corpo = """
                {"doadorId": %d, "itens": [{"itemId": %d, "quantidade": %d}]}
                """.formatted(doador.getId(), item.getId(), quantidade);
        MvcResult resultado = mockMvc.perform(post("/donations/items")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTRADA"))
                .andReturn();
        return primeiroId(resultado.getResponse().getContentAsString());
    }

    private void distribuir(String token, Item item, int quantidade) throws Exception {
        mockMvc.perform(post("/items/{id}/distributions", item.getId())
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": " + quantidade + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void fluxoCompleto_registrarDistribuirAjustarECancelar() throws Exception {
        String token = bearer(criarAdmin("admin@exemplo.com"));
        Donor doador = criarDoador();
        Item arroz = criarItem("Arroz");

        // 1. doação de 5 unidades: o estoque vai a 5
        long doacaoId = registrarDoacaoDeItens(token, doador, arroz, 5);
        assertThat(estoqueDe(arroz)).isEqualTo(5);

        // 2. distribui 2: o estoque vai a 3
        distribuir(token, arroz, 2);
        assertThat(estoqueDe(arroz)).isEqualTo(3);

        // 3. cancelar exigiria estornar 5 e só há 3: recusado, e nada muda
        mockMvc.perform(post("/donations/{id}/cancel", doacaoId)
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Erro de lançamento\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(donationRepository.findById(doacaoId).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.REGISTRADA);
        assertThat(estoqueDe(arroz)).isEqualTo(3);
        assertThat(movementRepository.count()).isEqualTo(2);

        // 4. ajuste de +2 (admin) devolve o estoque a 5
        mockMvc.perform(post("/items/{id}/adjustments", arroz.getId())
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidade\":2}"))
                .andExpect(status().isCreated());
        assertThat(estoqueDe(arroz)).isEqualTo(5);

        // 5. agora o cancelamento passa e o estoque zera
        mockMvc.perform(post("/donations/{id}/cancel", doacaoId)
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Erro de lançamento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Erro de lançamento"));
        assertThat(estoqueDe(arroz)).isZero();

        // 6. cancelar de novo é conflito
        mockMvc.perform(post("/donations/{id}/cancel", doacaoId)
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"De novo\"}"))
                .andExpect(status().isConflict());

        // 7. histórico completo (entrada, distribuição, ajuste, estorno) e estoque consistente
        assertThat(movementRepository.count()).isEqualTo(4);
        assertEstoqueConsistente();
    }

    @Test
    void cancelamentoRecusado_naoDeixaEstornoParcial() throws Exception {
        String token = bearer(criarAdmin("admin@exemplo.com"));
        Donor doador = criarDoador();
        Item arroz = criarItem("Arroz");
        Item feijao = criarItem("Feijão");

        String corpo = """
                {"doadorId": %d, "itens": [
                    {"itemId": %d, "quantidade": 5},
                    {"itemId": %d, "quantidade": 3}]}
                """.formatted(doador.getId(), arroz.getId(), feijao.getId());
        MvcResult resultado = mockMvc.perform(post("/donations/items")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn();
        long doacaoId = primeiroId(resultado.getResponse().getContentAsString());

        // todo o feijão é distribuído: o estorno dele vai falhar
        distribuir(token, feijao, 3);
        long movimentosAntes = movementRepository.count();

        mockMvc.perform(post("/donations/{id}/cancel", doacaoId)
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Tentativa\"}"))
                .andExpect(status().isUnprocessableEntity());

        // a transação foi desfeita por inteiro, inclusive o arroz, que poderia ter sido estornado antes
        assertThat(estoqueDe(arroz)).isEqualTo(5);
        assertThat(estoqueDe(feijao)).isZero();
        assertThat(movementRepository.count()).isEqualTo(movimentosAntes);
        assertThat(donationRepository.findById(doacaoId).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.REGISTRADA);
        assertEstoqueConsistente();
    }

    @Test
    void doacaoComItemInexistente_retorna404ENaoGravaNada() throws Exception {
        String token = bearer(criarAdmin("admin@exemplo.com"));
        Donor doador = criarDoador();
        Item arroz = criarItem("Arroz");

        String corpo = """
                {"doadorId": %d, "itens": [
                    {"itemId": %d, "quantidade": 5},
                    {"itemId": 99999, "quantidade": 1}]}
                """.formatted(doador.getId(), arroz.getId());
        mockMvc.perform(post("/donations/items")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound());

        assertThat(donationRepository.count()).isZero();
        assertThat(movementRepository.count()).isZero();
        assertThat(estoqueDe(arroz)).isZero();
    }

    @Test
    void doacaoEmDinheiro_naoMexeNoEstoque() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));
        Donor doador = criarDoador();

        mockMvc.perform(post("/donations/money")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doadorId\": %d, \"valor\": 150.50}".formatted(doador.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DINHEIRO"));

        assertThat(movementRepository.count()).isZero();
    }

    @Test
    void doacaoEmDinheiroComValorZero_retorna400() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));
        Donor doador = criarDoador();

        mockMvc.perform(post("/donations/money")
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doadorId\": %d, \"valor\": 0}".formatted(doador.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("valor"));
    }

    @Test
    void distribuirMaisDoQueOEstoque_retorna422() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));
        Item arroz = criarItem("Arroz");

        mockMvc.perform(post("/items/{id}/distributions", arroz.getId())
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 1}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(movementRepository.count()).isZero();
    }

    @Test
    void doadorComDoacoes_naoPodeSerExcluido_retorna409() throws Exception {
        User admin = criarAdmin("admin@exemplo.com");
        Donor doador = criarDoador();
        Item arroz = criarItem("Arroz");
        registrarDoacaoDeItens(bearer(admin), doador, arroz, 5);

        mockMvc.perform(delete("/donors/{id}", doador.getId()).header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());

        assertThat(donorRepository.existsById(doador.getId())).isTrue();
    }

    @Test
    void itemComHistorico_naoPodeSerExcluido_retorna409() throws Exception {
        User admin = criarAdmin("admin@exemplo.com");
        Item arroz = criarItem("Arroz");
        mockMvc.perform(post("/items/{id}/adjustments", arroz.getId())
                        .header(AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidade\":3}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/items/{id}", arroz.getId()).header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());

        assertThat(itemRepository.existsById(arroz.getId())).isTrue();
    }

    @Test
    void historicoDoItem_listaOsMovimentosDoMaisRecenteParaOMaisAntigo() throws Exception {
        String token = bearer(criarAdmin("admin@exemplo.com"));
        Item arroz = criarItem("Arroz");
        mockMvc.perform(post("/items/{id}/adjustments", arroz.getId())
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidade\":10}"))
                .andExpect(status().isCreated());
        distribuir(token, arroz, 4);

        mockMvc.perform(get("/items/{id}/movements", arroz.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].motivo").value("DISTRIBUICAO"))
                .andExpect(jsonPath("$.content[1].motivo").value("AJUSTE"));
    }
}