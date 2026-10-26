package com.ong.doacoes.integration;

import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 401 = não autenticado; 403 = autenticado, mas sem permissão. Passa pela cadeia de filtros real. */
class SecurityIntegrationTest extends IntegrationTestBase {

    // ---------- 401: sem autenticação válida ----------

    @Test
    void semToken_retorna401() throws Exception {
        mockMvc.perform(get("/donors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get("/donors").header(AUTHORIZATION, "Bearer isto.nao.e-um-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulterado_retorna401() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));
        String adulterado = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get("/donors").header(AUTHORIZATION, adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioQueNaoExisteMais_retorna401() throws Exception {
        User voluntario = criarVoluntario("vol@exemplo.com");
        String token = bearer(voluntario);
        userRepository.delete(voluntario);

        mockMvc.perform(get("/donors").header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 403: autenticado, sem permissão ----------

    @Test
    void voluntarioEmRotaDeAdmin_retorna403() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(get("/admin/users").header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNaoExcluiDoador_retorna403() throws Exception {
        Donor doador = criarDoador();
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(delete("/donors/{id}", doador.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNaoExcluiItem_retorna403() throws Exception {
        var item = criarItem("Arroz");
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(delete("/items/{id}", item.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNaoFazAjusteDeEstoque_retorna403() throws Exception {
        var item = criarItem("Arroz");
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(post("/items/{id}/adjustments", item.getId())
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidade\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNaoCancelaDoacao_retorna403() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(post("/donations/{id}/cancel", 1)
                        .header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"teste\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNaoVeRelatorio_retorna403() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(get("/reports/monthly").header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    // ---------- 2xx: com permissão ----------

    @Test
    void voluntarioConsultaDoadores_retorna200() throws Exception {
        String token = bearer(criarVoluntario("vol@exemplo.com"));

        mockMvc.perform(get("/donors").header(AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void adminAcessaRotaDeAdmin_retorna200() throws Exception {
        String token = bearer(criarAdmin("admin@exemplo.com"));

        mockMvc.perform(get("/admin/users").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminExcluiDoadorSemDoacoes_retorna204() throws Exception {
        Donor doador = criarDoador();
        String token = bearer(criarAdmin("admin@exemplo.com"));

        mockMvc.perform(delete("/donors/{id}", doador.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminVeRelatorio_retorna200() throws Exception {
        String token = bearer(criarAdmin("admin@exemplo.com"));

        mockMvc.perform(get("/reports/monthly").param("year", "2026").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.meses.length()").value(12));
    }

    @Test
    void cadastroELoginSaoPublicos() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carla\",\"email\":\"carla@exemplo.com\",\"senha\":\"Senha@12345\"}"))
                .andExpect(status().isCreated());
    }
}