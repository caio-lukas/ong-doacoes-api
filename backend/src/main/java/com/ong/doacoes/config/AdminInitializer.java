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
 * (ADMIN_EMAIL e ADMIN_PASSWORD). Também garante que exista pelo menos um administrador
 * ATIVO, usando as mesmas variáveis de ambiente. A senha nunca fica no código nem em migration.
 *
 * - Já existe um admin ativo: não faz nada.
 * - Não existe, e o e-mail das variáveis não está cadastrado: cria o admin.
 * - Não existe, e o e-mail já está cadastrado (por exemplo, o admin foi desativado):
 *   reativa essa conta como ADMIN, sem alterar a senha. É o caminho de recuperação
 *   para quando todos os admins ficam inativos: basta reiniciar a aplicação.
 *   Quem controla as variáveis de ambiente já controla o servidor, então não há risco novo.
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
        if (userRepository.existsByCargoAndAtivo(CargoUsuario.ADMIN, true)) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Não há administrador ativo e ADMIN_EMAIL/ADMIN_PASSWORD não foram definidos. "
                    + "Defina as variáveis de ambiente para criar ou recuperar o admin.");
            return;
        }

        String emailNormalizado = email.trim().toLowerCase();
        User existente = userRepository.findByEmail(emailNormalizado).orElse(null);

        if (existente == null) {
            userRepository.save(User.admin(nome, emailNormalizado, passwordEncoder.encode(senha)));
            log.info("Primeiro administrador criado: {}", emailNormalizado);
        } else {
            existente.promoverParaAdmin();
            existente.aprovar();
            userRepository.save(existente);
            log.warn("Nenhum admin ativo encontrado: {} foi reativado como administrador "
                    + "(a senha não foi alterada).", emailNormalizado);
        }
    }
}
