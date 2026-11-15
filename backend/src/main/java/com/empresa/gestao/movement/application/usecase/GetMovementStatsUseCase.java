package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.MovementStats;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.shared.domain.model.MovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetMovementStatsUseCase {

    private final StockMovementRepository movementRepository;

    public GetMovementStatsUseCase(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public MovementStats execute(UUID storeId) {
        return new MovementStats(
                movementRepository.countByStoreId(storeId),
                movementRepository.countByStoreIdAndType(storeId, MovementType.ENTRY),
                movementRepository.countByStoreIdAndType(storeId, MovementType.EXIT));
    }
}
