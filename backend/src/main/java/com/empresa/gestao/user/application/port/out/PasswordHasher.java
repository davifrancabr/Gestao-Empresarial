package com.empresa.gestao.user.application.port.out;

public interface PasswordHasher {

    String encode(String rawPassword);
}
