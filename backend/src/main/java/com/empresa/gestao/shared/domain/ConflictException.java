package com.empresa.gestao.shared.domain;

public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
