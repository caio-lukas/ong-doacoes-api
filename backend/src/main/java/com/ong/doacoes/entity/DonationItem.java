package com.ong.doacoes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "donation_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"donation_id", "item_id"}))
public class DonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantidade;

    protected DonationItem() {
    }

    DonationItem(Donation donation, Item item, int quantidade) {
        this.donation = donation;
        this.item = item;
        this.quantidade = quantidade;
    }

    // +-+ Getters +-+
    public Long getId() { return id; }
    public Donation getDonation() { return donation; }
    public Item getItem() { return item; }
    public int getQuantidade() { return quantidade; }
}
