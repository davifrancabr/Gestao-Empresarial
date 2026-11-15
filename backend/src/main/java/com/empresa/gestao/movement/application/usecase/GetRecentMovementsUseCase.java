package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetRecentMovementsUseCase {

    private final StockMovementRepository movementRepository;

    public GetRecentMovementsUseCase(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public List<MovementResult> execute(UUID storeId, int limit) {
        return movementRepository.findRecent(storeId, limit).stream()
                .map(MovementResultMapper::toResult)
                .toList();
    }
}
