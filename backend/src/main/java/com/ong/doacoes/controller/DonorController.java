package com.ong.doacoes.controller;

import com.ong.doacoes.dto.DonorRequest;
import com.ong.doacoes.dto.DonorResponse;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.service.DonorService;
import com.ong.doacoes.util.Paginas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Voluntário e admin cadastram, editam e consultam doadores; só o admin exclui. */
@RestController
@RequestMapping("/donors")
public class DonorController {

    private final DonorService service;

    public DonorController(DonorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonorResponse criar(@Valid @RequestBody DonorRequest request) {
        return service.criar(request);
    }

    /** Ex.: GET /donors?nome=mar&page=0&size=20 */
    @GetMapping
    public PageResponse<DonorResponse> listar(@RequestParam(required = false) String nome,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.listar(nome, Paginas.de(page, size, "nome"));
    }

    @GetMapping("/{id}")
    public DonorResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public DonorResponse atualizar(@PathVariable Long id, @Valid @RequestBody DonorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}