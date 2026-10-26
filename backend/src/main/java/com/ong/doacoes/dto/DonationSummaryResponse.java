package com.ong.doacoes.dto;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donation.DonationType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Versão resumida (sem itens) para listagens. */
public record DonationSummaryResponse(
        Long id,
        Long doadorId,
        String doadorNome,
        DonationType tipo,
        BigDecimal valor,
        LocalDate dataDoacao,
        DonationStatus status,
        String registradoPor) {

    public static DonationSummaryResponse from(Donation d) {
        return new DonationSummaryResponse(
                d.getId(),
                d.getDonor().getId(),
                d.getDonor().getNome(),
                d.getTipo(),
                d.getValor(),
                d.getDataDoacao(),
                d.getStatus(),
                d.getRegistradoPor().getNome());
    }
}