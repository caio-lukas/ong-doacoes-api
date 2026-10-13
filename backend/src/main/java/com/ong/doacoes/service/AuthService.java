package com.ong.doacoes.service;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.dto.LoginRequest;
import com.ong.doacoes.dto.LoginResponse;
import com.ong.doacoes.dto.RegisterRequest;
import com.ong.doacoes.dto.UserResponse;
import com.ong.doacoes.repository.UserRepository;
import com.ong.doacoes.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Cria sempre um VOLUNTARIO inativo: só consegue entrar depois que um admin aprovar. */
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        User user = new User(request.nome().trim(), email, passwordEncoder.encode(request.senha()));
        return UserResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        // Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails existem
        if (user == null || !passwordEncoder.matches(request.senha(), user.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        // Só depois de acertar a senha dizemos que o cadastro está pendente
        if (!user.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cadastro aguardando aprovação de um administrador");
        }
        return new LoginResponse(jwtService.generateToken(user.getEmail()), "Bearer",
                jwtService.getExpirationSeconds());
    }
}