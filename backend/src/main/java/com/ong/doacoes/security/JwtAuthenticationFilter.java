package com.ong.doacoes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê "Authorization: Bearer <token>", valida o token e, se o usuário existir e estiver ativo,
 * autentica a requisição. Se algo falhar, apenas segue sem autenticar: quem responde 401
 * é o próprio Spring Security.
 *
 * Esta classe NÃO é um @Component de propósito: o Spring Boot registraria o filtro duas vezes.
 * Ela é criada com "new" dentro do SecurityConfig.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIXO)) {
            String token = header.substring(PREFIXO.length());
            jwtService.extractEmail(token).ifPresent(email -> autenticar(email, request));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String email, HttpServletRequest request) {
        try {
            UserDetails usuario = userDetailsService.loadUserByUsername(email);
            if (!usuario.isEnabled()) {
                return; // desativado depois de receber o token: o token deixa de valer
            }
            var autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                    usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacao);
            SecurityContextHolder.setContext(contexto);
        } catch (UsernameNotFoundException ignorado) {
            // token válido de um usuário que não existe mais: segue sem autenticar
        }
    }
}