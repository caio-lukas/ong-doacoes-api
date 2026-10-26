package com.ong.doacoes.integration;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.entity.User.CargoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    private static String corpoDeCadastro(String nome, String email, String senha) {
        return "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(nome, email, senha);
    }

    private static String corpoDeLogin(String email, String senha) {
        return "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha);
    }

    @Test
    void fluxoCompleto_cadastroPendenteAprovacaoELogin() throws Exception {
        // 1. cadastro público: voluntário inativo
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCadastro("Carla", "carla@exemplo.com", SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargo").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.senha").doesNotExist());

        // 2. enquanto não for aprovada, não consegue entrar
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("carla@exemplo.com", SENHA)))
                .andExpect(status().isForbidden());

        // 3. o admin aprova
        User admin = criarAdmin("admin@exemplo.com");
        Long id = userRepository.findByEmail("carla@exemplo.com").orElseThrow().getId();
        mockMvc.perform(patch("/admin/users/{id}/approve", id).header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        // 4. agora o login funciona
        MvcResult login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("carla@exemplo.com", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn();
        String token = extrair(login.getResponse().getContentAsString(), "token");

        // 5. e o token dá acesso às rotas de voluntário
        mockMvc.perform(get("/donors").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroComEmailJaUsado_retorna409() throws Exception {
        criarVoluntario("carla@exemplo.com");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCadastro("Outra Carla", "carla@exemplo.com", SENHA)))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroComDadosInvalidos_retorna400ComListaDeErros() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCadastro("", "isto-nao-e-email", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isNotEmpty());
    }

    @Test
    void cadastroNuncaCriaAdmin_mesmoSeOCorpoTentarDefinirOCargo() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Esperta\",\"email\":\"esperta@exemplo.com\",\"senha\":\"" + SENHA
                        + "\",\"cargo\":\"ADMIN\"}"));

        // Pode ter sido recusado ou criado como voluntário; admin é que não pode ser
        CargoUsuario cargo = userRepository.findByEmail("esperta@exemplo.com")
                .map(User::getCargo)
                .orElse(CargoUsuario.VOLUNTARIO);
        assertThat(cargo).isEqualTo(CargoUsuario.VOLUNTARIO);
    }

    @Test
    void loginComSenhaErrada_retorna401() throws Exception {
        criarVoluntario("carla@exemplo.com");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("carla@exemplo.com", "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComEmailInexistente_retorna401ComAMesmaMensagemDaSenhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("ninguem@exemplo.com", SENHA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDesativaUsuario_otokenDeleDeixaDeValerNaHora() throws Exception {
        User admin = criarAdmin("admin@exemplo.com");
        User voluntario = criarVoluntario("vol@exemplo.com");
        String tokenDoVoluntario = bearer(voluntario);

        mockMvc.perform(get("/donors").header(AUTHORIZATION, tokenDoVoluntario))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/admin/users/{id}/deactivate", voluntario.getId())
                        .header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(get("/donors").header(AUTHORIZATION, tokenDoVoluntario))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPromoveVoluntario_aPermissaoNovaValeComOMesmoToken() throws Exception {
        User admin = criarAdmin("admin@exemplo.com");
        User voluntario = criarVoluntario("vol@exemplo.com");
        String token = bearer(voluntario);

        mockMvc.perform(get("/admin/users").header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/admin/users/{id}/promote", voluntario.getId())
                        .header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("ADMIN"));

        mockMvc.perform(get("/admin/users").header(AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void adminNaoDesativaAPropriaConta_retorna409() throws Exception {
        User admin = criarAdmin("admin@exemplo.com");

        mockMvc.perform(patch("/admin/users/{id}/deactivate", admin.getId())
                        .header(AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
    }
}