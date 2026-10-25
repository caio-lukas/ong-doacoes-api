package com.ong.doacoes.controller;

import com.ong.doacoes.dto.ItemRequest;
import com.ong.doacoes.dto.ItemResponse;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.service.ItemService;
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

/** Voluntário e admin cadastram, editam e consultam itens; só o admin exclui. */
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse criar(@Valid @RequestBody ItemRequest request) {
        return service.criar(request);
    }

    /** Ex.: GET /items?nome=arroz&categoria=alimentos&page=0&size=20 */
    @GetMapping
    public PageResponse<ItemResponse> listar(@RequestParam(required = false) String nome,
                                             @RequestParam(required = false) String categoria,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.listar(nome, categoria, Paginas.de(page, size, "nome"));
    }

    @GetMapping("/{id}")
    public ItemResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ItemResponse atualizar(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}