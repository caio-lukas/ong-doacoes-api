package com.ong.doacoes.controller;

import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.dto.CancelDonationRequest;
import com.ong.doacoes.dto.DonationResponse;
import com.ong.doacoes.dto.DonationSummaryResponse;
import com.ong.doacoes.dto.ItemsDonationRequest;
import com.ong.doacoes.dto.MoneyDonationRequest;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.service.DonationService;
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
 * Voluntário e admin registram e consultam doações; só o admin cancela.
 * Doação não é editada: se houve erro, cancela-se e registra-se de novo.
 */
@RestController
@RequestMapping("/donations")
public class DonationController {

    private final DonationService service;

    public DonationController(DonationService service) {
        this.service = service;
    }

    @PostMapping("/money")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse registrarDinheiro(@Valid @RequestBody MoneyDonationRequest request,
                                              @AuthenticationPrincipal UserDetails usuario) {
        return service.registrarDinheiro(request, usuario.getUsername());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse registrarItens(@Valid @RequestBody ItemsDonationRequest request,
                                           @AuthenticationPrincipal UserDetails usuario) {
        return service.registrarItens(request, usuario.getUsername());
    }

    /** Ex.: GET /donations?doadorId=1&status=REGISTRADA. Mais recentes primeiro. */
    @GetMapping
    public PageResponse<DonationSummaryResponse> listar(@RequestParam(required = false) Long doadorId,
                                                        @RequestParam(required = false) DonationStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return service.listar(doadorId, status, Paginas.decrescente(page, size, "dataDoacao"));
    }

    @GetMapping("/{id}")
    public DonationResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public DonationResponse cancelar(@PathVariable Long id,
                                     @Valid @RequestBody CancelDonationRequest request,
                                     @AuthenticationPrincipal UserDetails usuario) {
        return service.cancelar(id, request.motivo(), usuario.getUsername());
    }
}