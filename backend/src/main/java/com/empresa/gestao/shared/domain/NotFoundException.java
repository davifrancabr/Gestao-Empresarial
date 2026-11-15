package com.empresa.gestao.shared.domain;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
