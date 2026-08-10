package com.jclinical.auth.infra.security;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ValidateTokenUseCase validateTokenUseCase;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ValidateTokenUseCase validateTokenUseCase) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.validateTokenUseCase = validateTokenUseCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        log.debug("JwtAuthenticationFilter interceptando petición: {} {}", method, path);

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No se encontró cabecera Authorization válida para: {} {}, continuando filtro", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            boolean isTokenProviderValid = jwtTokenProvider.validateToken(token);
            boolean isUseCaseValid = validateTokenUseCase.validate(token);

            if (isTokenProviderValid && isUseCaseValid) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                log.debug("Token JWT válido para usuario '{}' en petición: {} {}", email, method, path);
                UserDetails principal = new User(email, "", Collections.emptyList());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Fallo de validación de token para petición: {} {}. ProviderValid: {}, UseCaseValid: {}",
                        method, path, isTokenProviderValid, isUseCaseValid);
            }
        } catch (Exception e) {
            log.error("Excepción al procesar token JWT para petición: {} {}", method, path, e);
        }

        filterChain.doFilter(request, response);
    }
}

