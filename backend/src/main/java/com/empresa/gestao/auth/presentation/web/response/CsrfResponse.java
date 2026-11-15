package com.empresa.gestao.auth.presentation.web.response;

public record CsrfResponse(String headerName, String token) {
}
