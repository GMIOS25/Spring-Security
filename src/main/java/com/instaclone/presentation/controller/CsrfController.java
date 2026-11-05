package com.instaclone.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

    /**
     * REST endpoint providing CSRF token for Single Page Applications (SPA).
     * The token returned is masked using XOR to defend against BREACH attacks.
     */
    @GetMapping("/token")
    public CsrfToken getCsrfToken(HttpServletRequest request, CsrfToken token) {
        if (token != null) {
            return token;
        }
        CsrfToken reqToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (reqToken == null) {
            reqToken = (CsrfToken) request.getAttribute("_csrf");
        }
        return reqToken;
    }
}
