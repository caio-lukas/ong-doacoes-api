package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donation.DonationType;
import com.ong.doacoes.dto.MonthlyReportResponse;
import com.ong.doacoes.exception.BusinessRuleException;
import com.ong.doacoes.repository.DonationRepository;
import com.ong.doacoes.repository.MonthlyTotalProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private DonationRepository repository;

    @InjectMocks
    private ReportService service;

    /** Linha de resultado simulada, no lugar do que o banco devolveria. */
    private record Linha(Integer mes, BigDecimal total, Long quantidade) implements MonthlyTotalProjection {
        @Override
        public Integer getMes() {
            return mes;
        }

        @Override
        public BigDecimal getTotal() {
            return total;
        }

        @Override
        public Long getQuantidade() {
            return quantidade;
        }
    }

    @Test
    void arrecadacaoMensal_completaOsMesesSemDoacaoComZeroESomaOAno() {
        when(repository.totaisPorMes(any(), any(), any(), any())).thenReturn(List.of(
                new Linha(1, new BigDecimal("100.00"), 2L),
                new Linha(3, new BigDecimal("50.50"), 1L)));

        MonthlyReportResponse relatorio = service.arrecadacaoMensal(2026);

        assertThat(relatorio.ano()).isEqualTo(2026);
        assertThat(relatorio.meses()).hasSize(12);
        assertThat(relatorio.meses().get(0).total()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(relatorio.meses().get(0).nomeMes()).isEqualTo("janeiro");
        assertThat(relatorio.meses().get(1).total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(relatorio.meses().get(1).quantidadeDoacoes()).isZero();
        assertThat(relatorio.meses().get(2).total()).isEqualByComparingTo(new BigDecimal("50.50"));
        assertThat(relatorio.totalAno()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(relatorio.quantidadeDoacoes()).isEqualTo(3);
    }

    @Test
    void arrecadacaoMensal_consultaSoDinheiroRegistradasNoIntervaloDoAno() {
        when(repository.totaisPorMes(any(), any(), any(), any())).thenReturn(List.of());

        service.arrecadacaoMensal(2026);

        verify(repository).totaisPorMes(
                eq(DonationType.DINHEIRO),
                eq(DonationStatus.REGISTRADA),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void arrecadacaoMensal_anoSemDoacoesDevolveDozeMesesZerados() {
        when(repository.totaisPorMes(any(), any(), any(), any())).thenReturn(List.of());

        MonthlyReportResponse relatorio = service.arrecadacaoMensal(2026);

        assertThat(relatorio.meses()).hasSize(12);
        assertThat(relatorio.totalAno()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(relatorio.quantidadeDoacoes()).isZero();
    }

    @Test
    void arrecadacaoMensal_semAnoUsaOAnoCorrente() {
        int anoAtual = Year.now().getValue();
        when(repository.totaisPorMes(any(), any(), any(), any())).thenReturn(List.of());

        MonthlyReportResponse relatorio = service.arrecadacaoMensal(null);

        assertThat(relatorio.ano()).isEqualTo(anoAtual);
        verify(repository).totaisPorMes(any(), any(),
                eq(LocalDate.of(anoAtual, 1, 1)), eq(LocalDate.of(anoAtual + 1, 1, 1)));
    }

    @Test
    void arrecadacaoMensal_recusaAnoForaDoIntervaloPermitido() {
        assertThatThrownBy(() -> service.arrecadacaoMensal(1999))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ano inválido");
        assertThatThrownBy(() -> service.arrecadacaoMensal(2101))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(repository);
    }
}