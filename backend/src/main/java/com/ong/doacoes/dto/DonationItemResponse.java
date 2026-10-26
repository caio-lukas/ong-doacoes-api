package com.ong.doacoes.dto;

import com.ong.doacoes.entity.DonationItem;

public record DonationItemResponse(
        Long itemId,
        String itemNome,
        String unidade,
        int quantidade) {

    public static DonationItemResponse from(DonationItem di) {
        return new DonationItemResponse(
                di.getItem().getId(), di.getItem().getNome(), di.getItem().getUnidade(), di.getQuantidade());
    }
}