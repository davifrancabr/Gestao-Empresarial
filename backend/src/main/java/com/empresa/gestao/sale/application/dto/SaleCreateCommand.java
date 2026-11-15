package com.empresa.gestao.sale.application.dto;

import java.util.List;
import java.util.UUID;

public record SaleCreateCommand(UUID customerId, List<SaleItemCommand> items) {
}
