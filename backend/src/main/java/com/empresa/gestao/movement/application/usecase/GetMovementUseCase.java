package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.domain.model.StockMovement;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetMovementUseCase {

    private final StockMovementRepository movementRepository;

    public GetMovementUseCase(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public MovementResult execute(UUID storeId, UUID id) {
        StockMovement movement = movementRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException("Movimentação não encontrada."));
        return MovementResultMapper.toResult(movement);
    }
}
