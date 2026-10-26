package com.ong.doacoes.integration;

import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.entity.Item;
import com.ong.doacoes.entity.User;
import com.ong.doacoes.repository.DonationRepository;
import com.ong.doacoes.repository.DonorRepository;
import com.ong.doacoes.repository.ItemRepository;
import com.ong.doacoes.repository.StockMovementRepository;
import com.ong.doacoes.repository.UserRepository;
import com.ong.doacoes.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base dos testes de integração: sobe a aplicação inteira contra um PostgreSQL real em container.
 * Cada teste começa com o banco limpo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String SENHA = "Senha@12345";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected DonorRepository donorRepository;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected DonationRepository donationRepository;
    @Autowired
    protected StockMovementRepository movementRepository;

    @BeforeEach
    protected void limparBanco() {
        jdbc.execute("TRUNCATE TABLE stock_movements, donation_items, donations, items, donors, users "
                + "RESTART IDENTITY CASCADE");
    }

    // ---------- dados de apoio ----------

    protected User criarAdmin(String email) {
        return userRepository.save(User.admin("Admin", email, passwordEncoder.encode(SENHA)));
    }

    /** Voluntário já aprovado (ativo). */
    protected User criarVoluntario(String email) {
        User usuario = new User("Voluntário", email, passwordEncoder.encode(SENHA));
        usuario.aprovar();
        return userRepository.save(usuario);
    }

    protected Donor criarDoador() {
        return donorRepository.save(new Donor("Maria Silva", "maria@exemplo.com", "22999990000"));
    }

    protected Item criarItem(String nome) {
        return itemRepository.save(new Item(nome, "Alimentos", "kg"));
    }

    protected int estoqueDe(Item item) {
        return itemRepository.findById(item.getId()).orElseThrow().getQuantidadeEstoque();
    }

    /** Valor do cabeçalho Authorization com um token válido para o usuário. */
    protected String bearer(User usuario) {
        return "Bearer " + jwtService.generateToken(usuario.getEmail());
    }

    // ---------- leitura de JSON ----------

    /** Primeiro valor de um campo de texto. Ex.: extrair(json, "token"). */
    protected static String extrair(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo '" + campo + "' não encontrado em: " + json);
        }
        return m.group(1);
    }

    /** O primeiro "id" numérico da resposta. */
    protected static long primeiroId(String json) {
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo 'id' não encontrado em: " + json);
        }
        return Long.parseLong(m.group(1));
    }

    // ---------- invariante do estoque ----------

    /** A quantidade de cada item deve ser igual à soma das entradas menos as saídas. */
    protected void assertEstoqueConsistente() {
        List<Map<String, Object>> divergentes = jdbc.queryForList("""
                SELECT i.id, i.nome, i.quantidade_estoque
                FROM items i
                LEFT JOIN stock_movements m ON m.item_id = i.id
                GROUP BY i.id, i.nome, i.quantidade_estoque
                HAVING i.quantidade_estoque <> COALESCE(
                    SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0)
                """);
        assertThat(divergentes).as("itens com estoque diferente do histórico").isEmpty();
    }
}