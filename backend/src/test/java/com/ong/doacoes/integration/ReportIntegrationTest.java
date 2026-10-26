package com.ong.doacoes.integration;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.entity.Item;
import com.ong.doacoes.entity.User;
import com.ong.doacoes.dto.MonthlyReportResponse;
import com.ong.doacoes.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Executa a consulta de totais por mês contra um PostgreSQL de verdade. */
class ReportIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReportService reportService;

    private Donation salvarDinheiro(Donor doador, User usuario, String valor, LocalDate data) {
        return donationRepository.save(Donation.emDinheiro(doador, new BigDecimal(valor), data, usuario));
    }

    @Test
    void totaisPorMes_contamSoDinheiroRegistradoDoAnoConsultado() {
        User admin = criarAdmin("admin@exemplo.com");
        Donor doador = criarDoador();
        Item arroz = criarItem("Arroz");

        salvarDinheiro(doador, admin, "100.00", LocalDate.of(2026, 1, 10));
        salvarDinheiro(doador, admin, "50.50", LocalDate.of(2026, 1, 25));
        salvarDinheiro(doador, admin, "200.00", LocalDate.of(2026, 3, 5));

        // fora do ano consultado
        salvarDinheiro(doador, admin, "999.00", LocalDate.of(2025, 12, 31));
        salvarDinheiro(doador, admin, "999.00", LocalDate.of(2027, 1, 1));

        // cancelada: não conta
        Donation cancelada = salvarDinheiro(doador, admin, "300.00", LocalDate.of(2026, 3, 10));
        cancelada.cancelar(admin, "Erro de lançamento");
        donationRepository.save(cancelada);

        // doação de itens no mesmo mês: não conta
        Donation deItens = Donation.deItens(doador, LocalDate.of(2026, 3, 15), admin);
        deItens.adicionarItem(arroz, 5);
        donationRepository.save(deItens);

        MonthlyReportResponse relatorio = reportService.arrecadacaoMensal(2026);

        assertThat(relatorio.meses()).hasSize(12);
        assertThat(relatorio.meses().get(0).total()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(relatorio.meses().get(0).quantidadeDoacoes()).isEqualTo(2);
        assertThat(relatorio.meses().get(1).total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(relatorio.meses().get(2).total()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(relatorio.meses().get(2).quantidadeDoacoes()).isEqualTo(1);
        assertThat(relatorio.totalAno()).isEqualByComparingTo(new BigDecimal("350.50"));
        assertThat(relatorio.quantidadeDoacoes()).isEqualTo(3);
    }

    @Test
    void anoSemDoacoes_devolveDozeMesesZerados() {
        MonthlyReportResponse relatorio = reportService.arrecadacaoMensal(2026);

        assertThat(relatorio.meses()).hasSize(12);
        assertThat(relatorio.totalAno()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}