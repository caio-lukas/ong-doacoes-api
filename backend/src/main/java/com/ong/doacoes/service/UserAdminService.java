package com.ong.doacoes.service;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.dto.UserResponse;
import com.ong.doacoes.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Gestão de usuários pelo admin: aprovar, desativar e promover. Usuários nunca são excluídos. */
@Service
public class UserAdminService {

    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listar(Boolean ativo, Pageable pageable) {
        Page<User> pagina = (ativo == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByAtivo(ativo, pageable);
        return PageResponse.from(pagina.map(UserResponse::from));
    }

    @Transactional
    public UserResponse aprovar(Long id) {
        User user = buscar(id);
        user.aprovar();   // o JPA grava a mudança ao final da transação (dirty checking)
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse desativar(Long id, String emailDoAdminLogado) {
        User user = buscar(id);
        if (user.getEmail().equals(emailDoAdminLogado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode desativar a própria conta");
        }
        user.desativar();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse promover(Long id) {
        User user = buscar(id);
        user.promoverParaAdmin();
        return UserResponse.from(user);
    }

    private User buscar(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}