package com.empresa.gestao.movement.domain.repository;

import com.empresa.gestao.movement.domain.repository.DailyMovementCount;
import com.empresa.gestao.movement.domain.repository.MovementFilter;
import com.empresa.gestao.movement.domain.model.StockMovement;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.MovementType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    Optional<StockMovement> findByIdAndStoreId(UUID id, UUID storeId);

    PageResult<StockMovement> search(MovementFilter filter, PageQuery pageQuery);

    List<StockMovement> findRecent(UUID storeId, int limit);

    long countByStoreId(UUID storeId);

    long countByStoreIdAndType(UUID storeId, MovementType type);

    List<TopMovedProduct> findTopMoved(UUID storeId, int limit);

    List<TopMovedProduct> findTopMovedSince(UUID storeId, Instant since, int limit);

    List<DailyMovementCount> countDaily(UUID storeId, Instant since);
}
