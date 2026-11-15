package com.empresa.gestao.movement.application.dto;

import java.time.LocalDate;

public record TrendPoint(LocalDate date, long entries, long exits) {
}
