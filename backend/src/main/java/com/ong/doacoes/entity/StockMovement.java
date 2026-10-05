package com.ong.doacoes.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Histórico de estoque: registro imutável de cada entrada ou saída de um item.
 * Nunca é editado nem excluído. Uma doação cancelada gera movimentações de SAIDA
 * com motivo CANCELAMENTO_DOACAO, em vez de apagar as de ENTRADA.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private MovementType tipo;
    public enum MovementType {
        ENTRADA,
        SAIDA
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private MovementReason motivo;
    public enum MovementReason {
        DOACAO,
        CANCELAMENTO_DOACAO,
        DISTRIBUICAO,
        AJUSTE
    }

    @Column(nullable = false, updatable = false)
    private int quantidade;

    /** Doação de origem, quando houver (entrada por doação ou estorno por cancelamento). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", updatable = false)
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false, updatable = false)
    private User registradoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected StockMovement() {
    }

    public StockMovement(Item item, MovementType tipo, MovementReason motivo,
                         int quantidade, Donation donation, User registradoPor) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        this.item = item;
        this.tipo = tipo;
        this.motivo = motivo;
        this.quantidade = quantidade;
        this.donation = donation;
        this.registradoPor = registradoPor;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public MovementType getTipo() { return tipo; }
    public MovementReason getMotivo() { return motivo; }
    public int getQuantidade() { return quantidade; }
    public Donation getDonation() { return donation; }
    public User getRegistradoPor() { return registradoPor; }
    public Instant getCriadoEm() { return criadoEm; }
}