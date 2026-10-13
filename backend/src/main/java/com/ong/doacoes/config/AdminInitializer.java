package com.ong.doacoes.config;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.entity.User.CargoUsuario;
import com.ong.doacoes.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador na inicialização, a partir de variáveis de ambiente
 * (ADMIN_EMAIL e ADMIN_PASSWORD). Não faz nada se já existir um admin.
 * A senha nunca fica no código nem em migration.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.nome}") String nome,
                            @Value("${app.admin.email}") String email,
                            @Value("${app.admin.senha}") String senha) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByCargo(CargoUsuario.ADMIN)) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Não há administrador cadastrado e ADMIN_EMAIL/ADMIN_PASSWORD não foram definidos. "
                    + "Defina as variáveis de ambiente para criar o primeiro admin.");
            return;
        }
        userRepository.save(User.admin(nome, email, passwordEncoder.encode(senha)));
        log.info("Primeiro administrador criado: {}", email);
    }
}