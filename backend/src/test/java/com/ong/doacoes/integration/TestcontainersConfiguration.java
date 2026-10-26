package com.ong.doacoes.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe um PostgreSQL descartável em Docker para os testes de integração. O @ServiceConnection
 * liga o DataSource e o Flyway a esse container automaticamente. Os testes que importam esta
 * classe compartilham o mesmo contexto Spring e, portanto, o mesmo container.
 *
 * Requer o Docker em execução. Usa a mesma versão do banco do docker-compose.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }
}