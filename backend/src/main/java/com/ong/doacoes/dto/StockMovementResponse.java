package com.ong.doacoes.dto;

import com.ong.doacoes.entity.StockMovement;
import com.ong.doacoes.entity.StockMovement.MovementReason;
import com.ong.doacoes.entity.StockMovement.MovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long itemId,
        MovementType tipo,
        MovementReason motivo,
        int quantidade,
        Long doacaoId,
        String registradoPor,
        Instant criadoEm) {

    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getItem().getId(),
                m.getTipo(),
                m.getMotivo(),
                m.getQuantidade(),
                m.getDonation() == null ? null : m.getDonation().getId(),
                m.getRegistradoPor().getNome(),
                m.getCriadoEm());
    }
}