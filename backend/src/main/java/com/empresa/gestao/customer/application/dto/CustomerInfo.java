package com.empresa.gestao.customer.application.dto;

import java.util.UUID;

public record CustomerInfo(UUID id, UUID storeId, String name, boolean active) {
}
