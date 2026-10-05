package com.ong.doacoes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationType tipo;
    public enum DonationType {
        DINHEIRO,
        ITENS
    }

    @Column(precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_doacao", nullable = false)
    private LocalDate dataDoacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status;
    public enum DonationStatus {
        REGISTRADA,
        CANCELADA
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por")
    private User canceladaPor;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DonationItem> itens = new ArrayList<>();

    protected Donation() {
    }

    private Donation(Donor donor, DonationType tipo, BigDecimal valor,
                     LocalDate dataDoacao, User registradoPor) {
        this.donor = donor;
        this.tipo = tipo;
        this.valor = valor;
        this.dataDoacao = dataDoacao;
        this.registradoPor = registradoPor;
        this.status = DonationStatus.REGISTRADA;
    }

    public static Donation emDinheiro(Donor donor, BigDecimal valor,
                                      LocalDate dataDoacao, User registradoPor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor da doação deve ser maior que zero");
        }
        return new Donation(donor, DonationType.DINHEIRO,
                valor.setScale(2, RoundingMode.HALF_UP), dataDoacao, registradoPor);
    }

    public static Donation deItens(Donor donor, LocalDate dataDoacao, User registradoPor) {
        return new Donation(donor, DonationType.ITENS, null, dataDoacao, registradoPor);
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
    }

    /** Adiciona um item à doação (ainda não persistida). O estoque é atualizado pelo serviço. */
    public DonationItem adicionarItem(Item item, int quantidade) {
        if (tipo != DonationType.ITENS) {
            throw new IllegalStateException("Doação em dinheiro não possui itens");
        }
        if (status != DonationStatus.REGISTRADA) {
            throw new IllegalStateException("Não é possível alterar uma doação cancelada");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        boolean repetido = itens.stream()
                .anyMatch(i -> i.getItem().getId() != null && i.getItem().getId().equals(item.getId()));
        if (repetido) {
            throw new IllegalArgumentException("O item '" + item.getNome() + "' já está na doação");
        }
        DonationItem novo = new DonationItem(this, item, quantidade);
        itens.add(novo);
        return novo;
    }

    /** Marca como cancelada. O estorno do estoque é responsabilidade do serviço (mesma transação). */
    public void cancelar(User por, String motivo) {
        if (status == DonationStatus.CANCELADA) {
            throw new IllegalStateException("A doação já está cancelada");
        }
        this.status = DonationStatus.CANCELADA;
        this.canceladaPor = por;
        this.canceladaEm = Instant.now();
        this.motivoCancelamento = motivo;
    }

    public Long getId() { return id; }
    public Donor getDonor() { return donor; }
    public DonationType getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getDataDoacao() { return dataDoacao; }
    public DonationStatus getStatus() { return status; }
    public User getRegistradoPor() { return registradoPor; }
    public User getCanceladaPor() { return canceladaPor; }
    public Instant getCanceladaEm() { return canceladaEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public Instant getCriadoEm() { return criadoEm; }
    public List<DonationItem> getItens() { return Collections.unmodifiableList(itens); }
}