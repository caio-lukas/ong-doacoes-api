package com.ong.doacoes.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Doador: apenas um cadastro (quem doou). Não faz login e não é um {@link User}.
 * Doador com doações registradas não pode ser excluído (a chave estrangeira impede).
 */

@Entity(name = "Donor")
@Table(name = "donors")
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "email", length = 160, nullable = false)
    private String email;

    @Column(name = "telefone", length = 20, nullable = false)
    private String telefone;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
    }

    protected Donor() {

    }

    public Donor(String nome, String email, String telefone) {
        atualizar(nome, email, telefone);
    }

    public void atualizar(String nome, String email, String telefone) {
        String emailLimpo = normalizar(email);
        String telefoneLimpo = normalizar(telefone);
        if (emailLimpo == null || telefoneLimpo == null) {
            throw new IllegalArgumentException("Informe seus contatos (e-mail e telefone)");
        }
        this.nome = nome;
        this.email = emailLimpo;
        this.telefone = telefoneLimpo;
    }

    private static String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim().toLowerCase();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
