package com.ong.doacoes;

import com.ong.doacoes.integration.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe a aplicação contra um PostgreSQL em container. Se passar, as migrations do Flyway
 * rodam do zero e o Hibernate (ddl-auto: validate) confirma que entidades e tabelas batem.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DoacoesApplicationTests {

	@Test
	void contextoCarregaEMigrationsBatemComAsEntidades() {
	}
}