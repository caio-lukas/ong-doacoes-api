package com.ong.doacoes.security;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega o usuário do banco para o Spring Security. Usuário não aprovado (ativo = false)
 * vira "disabled". Ter este bean também desliga o usuário/senha gerados automaticamente.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getSenha())
                .roles(user.getCargo().name())   // vira ROLE_ADMIN ou ROLE_VOLUNTARIO
                .disabled(!user.isAtivo())
                .build();
    }
}