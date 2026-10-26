package com.ong.doacoes.dto;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donation.DonationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Detalhe completo de uma doação, com os itens e os dados de cancelamento. */
public record DonationResponse(
        Long id,
        Long doadorId,
        String doadorNome,
        DonationType tipo,
        BigDecimal valor,
        LocalDate dataDoacao,
        DonationStatus status,
        String registradoPor,
        Instant criadoEm,
        String canceladaPor,
        Instant canceladaEm,
        String motivoCancelamento,
        List<DonationItemResponse> itens) {

    public static DonationResponse from(Donation d) {
        return new DonationResponse(
                d.getId(),
                d.getDonor().getId(),
                d.getDonor().getNome(),
                d.getTipo(),
                d.getValor(),
                d.getDataDoacao(),
                d.getStatus(),
                d.getRegistradoPor().getNome(),
                d.getCriadoEm(),
                d.getCanceladaPor() == null ? null : d.getCanceladaPor().getNome(),
                d.getCanceladaEm(),
                d.getMotivoCancelamento(),
                d.getItens().stream().map(DonationItemResponse::from).toList());
    }
}