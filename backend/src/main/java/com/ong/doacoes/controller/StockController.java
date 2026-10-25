package com.ong.doacoes.controller;

import com.ong.doacoes.dto.AdjustmentRequest;
import com.ong.doacoes.dto.DistributionRequest;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.dto.StockMovementResponse;
import com.ong.doacoes.service.StockService;
import com.ong.doacoes.util.Paginas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Movimentações de estoque de um item. Voluntário e admin registram distribuições e consultam
 * o histórico; apenas o admin faz ajustes manuais.
 */
@RestController
@RequestMapping("/items/{itemId}")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @PostMapping("/distributions")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse distribuir(@PathVariable Long itemId,
                                            @Valid @RequestBody DistributionRequest request,
                                            @AuthenticationPrincipal UserDetails usuario) {
        return service.distribuir(itemId, request.quantidade(), usuario.getUsername());
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse ajustar(@PathVariable Long itemId,
                                         @Valid @RequestBody AdjustmentRequest request,
                                         @AuthenticationPrincipal UserDetails usuario) {
        return service.ajustar(itemId, request.tipo(), request.quantidade(), usuario.getUsername());
    }

    /** Mais recentes primeiro. */
    @GetMapping("/movements")
    public PageResponse<StockMovementResponse> historico(@PathVariable Long itemId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return service.historico(itemId, Paginas.decrescente(page, size, "criadoEm"));
    }
}