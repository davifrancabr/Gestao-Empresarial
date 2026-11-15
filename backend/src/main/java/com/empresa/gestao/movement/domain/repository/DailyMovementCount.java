package com.empresa.gestao.movement.domain.repository;

import com.empresa.gestao.shared.domain.model.MovementType;

import java.time.LocalDate;

public record DailyMovementCount(LocalDate day, MovementType type, long total) {
}
