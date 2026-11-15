package com.empresa.gestao.sale.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemCommand(UUID productId, Integer quantity, BigDecimal unitPrice) {
}
