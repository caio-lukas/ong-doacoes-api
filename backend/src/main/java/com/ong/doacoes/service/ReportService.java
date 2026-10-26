package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donation.DonationType;
import com.ong.doacoes.dto.MonthlyReportResponse;
import com.ong.doacoes.dto.MonthlyTotalResponse;
import com.ong.doacoes.exception.BusinessRuleException;
import com.ong.doacoes.repository.DonationRepository;
import com.ong.doacoes.repository.MonthlyTotalProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final int ANO_MINIMO = 2000;
    private static final int ANO_MAXIMO = 2100;

    private final DonationRepository repository;

    public ReportService(DonationRepository repository) {
        this.repository = repository;
    }

    /**
     * Arrecadação mensal em dinheiro. Conta só doações em DINHEIRO com status REGISTRADA:
     * as canceladas e as doações de itens ficam de fora. Sem o ano, usa o ano corrente.
     */
    @Transactional(readOnly = true)
    public MonthlyReportResponse arrecadacaoMensal(Integer ano) {
        int anoConsultado = (ano != null) ? ano : Year.now().getValue();
        if (anoConsultado < ANO_MINIMO || anoConsultado > ANO_MAXIMO) {
            throw new BusinessRuleException("Ano inválido: informe um ano entre " + ANO_MINIMO + " e " + ANO_MAXIMO);
        }

        LocalDate inicio = LocalDate.of(anoConsultado, 1, 1);
        List<MonthlyTotalProjection> linhas = repository.totaisPorMes(
                DonationType.DINHEIRO, DonationStatus.REGISTRADA, inicio, inicio.plusYears(1));

        Map<Integer, MonthlyTotalProjection> porMes = new HashMap<>();
        for (MonthlyTotalProjection linha : linhas) {
            porMes.put(linha.getMes(), linha);
        }

        // Sempre devolve os 12 meses: o front não precisa tratar mês ausente
        List<MonthlyTotalResponse> meses = new ArrayList<>(12);
        BigDecimal totalAno = BigDecimal.ZERO.setScale(2);
        long quantidadeAno = 0;
        for (int mes = 1; mes <= 12; mes++) {
            MonthlyTotalProjection linha = porMes.get(mes);
            BigDecimal total = (linha == null || linha.getTotal() == null)
                    ? BigDecimal.ZERO.setScale(2)
                    : linha.getTotal().setScale(2, RoundingMode.HALF_UP);
            long quantidade = (linha == null || linha.getQuantidade() == null) ? 0 : linha.getQuantidade();

            meses.add(MonthlyTotalResponse.of(mes, total, quantidade));
            totalAno = totalAno.add(total);
            quantidadeAno += quantidade;
        }
        return new MonthlyReportResponse(anoConsultado, totalAno, quantidadeAno, meses);
    }
}