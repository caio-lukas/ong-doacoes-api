package com.ong.doacoes.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Item de estoque. A quantidade só deve mudar junto com um {@link StockMovement},
 * dentro da mesma transação.
 *
 * Concorrência: @Version ativa o bloqueio otimista. Se duas transações alterarem o mesmo
 * item ao mesmo tempo, a segunda falha com ObjectOptimisticLockingFailureException.
 *
 * A quantidade é inteira. Se precisar de decimais (ex.: 2,5 kg), troque por BigDecimal.
 */

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 120)
    private String nome;

    @Column(name = "categoria", nullable = false, length = 60)
    private String categoria;

    @Column(name = "unidade", nullable = false, length = 20)
    private String unidade;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Item() {

    }

    public Item(String nome, String categoria, String unidade) {
        atualizar(nome, categoria, unidade);
        this.quantidadeEstoque = 0;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
    }

    public void atualizar(String nome, String categoria, String unidade) {
        this.nome = nome;
        this.categoria = categoria;
        this.unidade = unidade;
    }

    public void adicionarEstoque(int quantidade) {
        exigirPositivo(quantidade);
        this.quantidadeEstoque += quantidade;
    }

    public void removerEstoque(int quantidade) {
        exigirPositivo(quantidade);
        if (quantidade > this.quantidadeEstoque) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente de '" + nome + "': disponível " + quantidadeEstoque
                    + ", solicitado " + quantidade);
        }
        this.quantidadeEstoque -= quantidade;
    }

    private static void exigirPositivo(int quantidade) {
        if(quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
    }

    // +-+ Getters +-+
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getUnidade() {
        return unidade;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public long getVersao() {
        return versao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
