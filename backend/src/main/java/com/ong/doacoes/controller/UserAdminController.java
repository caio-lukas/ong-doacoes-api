package com.ong.doacoes.controller;

import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.dto.UserResponse;
import com.ong.doacoes.service.UserAdminService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Todas as rotas /admin/** exigem cargo ADMIN (regra no SecurityConfig). */
@RestController
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    /** Ex.: GET /admin/users?ativo=false lista os cadastros pendentes de aprovação. */
    @GetMapping
    public PageResponse<UserResponse> listar(@RequestParam(required = false) Boolean ativo,
                                             @PageableDefault(size = 20, sort = "criadoEm") Pageable pageable) {
        return service.listar(ativo, pageable);
    }

    @PatchMapping("/{id}/approve")
    public UserResponse aprovar(@PathVariable Long id) {
        return service.aprovar(id);
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponse desativar(@PathVariable Long id, @AuthenticationPrincipal UserDetails admin) {
        return service.desativar(id, admin.getUsername());
    }

    @PatchMapping("/{id}/promote")
    public UserResponse promover(@PathVariable Long id) {
        return service.promover(id);
    }
}