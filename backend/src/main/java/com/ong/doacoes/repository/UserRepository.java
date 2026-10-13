package com.ong.doacoes.repository;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.entity.User.CargoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCargo(CargoUsuario cargo);

    Page<User> findByAtivo(boolean ativo, Pageable pageable);
}