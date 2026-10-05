package com.ong.doacoes.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Usuário do sistema (admin ou voluntário). Doador NÃO é usuário: {@link Donor}.
 *
 * Observações:
 * - A tabela se chama "users" porque "user" é palavra reservada no PostgreSQL.
 * - O registro público cria sempre VOLUNTARIO com ativo = false; o admin aprova depois.
 * - Usuários nunca são excluídos, apenas desativados, pois aparecem como autores de registros.
 */

@Entity(name = "User")
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", length = 120, nullable = false)
    private String nome;

    @Column(name = "email", unique = true, length = 160, nullable = false)
    private String email;

    @Column(name = "senha", nullable = false, length = 100)
    private String senha;

    @Column(name = "cargo", nullable = false)
    @Enumerated(EnumType.STRING)
    private CargoUsuario cargo;
    public enum CargoUsuario {
        ADMIN,
        VOLUNTARIO
    }

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected User() {
    }

    public User(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email.trim().toLowerCase();
        this.senha = senha;
        this.cargo = CargoUsuario.VOLUNTARIO;
        this.ativo = false;
    }

    public static User admin(String nome, String email, String senha) {
        User u = new User(nome, email, senha);
        u.cargo = CargoUsuario.ADMIN;
        u.ativo = true;
        return u;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
    }

    public void aprovar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void promoverParaAdmin() {
        this.cargo = CargoUsuario.ADMIN;
    }

    // +-+ Getters +-+
    public Instant getCriadoEm() {
        return criadoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public CargoUsuario getCargo() {
        return cargo;
    }

    public String getSenha() {
        return senha;
    }

    public String getEmail() {
        return email;
    }

    public String getNome() {
        return nome;
    }

    public Long getId() {
        return id;
    }
}
