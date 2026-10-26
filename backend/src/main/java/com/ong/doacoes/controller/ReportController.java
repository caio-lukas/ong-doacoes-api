package com.ong.doacoes.controller;

import com.ong.doacoes.dto.MonthlyReportResponse;
import com.ong.doacoes.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relatórios: apenas para o admin. */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    /** Ex.: GET /reports/monthly?year=2026. Sem o parâmetro, usa o ano corrente. */
    @GetMapping("/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public MonthlyReportResponse arrecadacaoMensal(@RequestParam(required = false) Integer year) {
        return service.arrecadacaoMensal(year);
    }
}