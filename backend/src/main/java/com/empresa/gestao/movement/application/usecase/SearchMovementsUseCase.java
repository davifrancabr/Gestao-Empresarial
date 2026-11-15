package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.domain.repository.MovementFilter;
import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class SearchMovementsUseCase {

    private final StockMovementRepository movementRepository;

    public SearchMovementsUseCase(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<MovementResult> execute(UUID storeId, UUID productId, MovementType type,
                                              LocalDate from, LocalDate to, String search,
                                              Integer page, Integer size, String sort) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        PageQuery query = PageQuery.of(page, size, sort,
                Map.of("createdAt", "createdAt"), "createdAt,desc");
        MovementFilter filter = new MovementFilter(storeId, productId, type, fromInstant, toInstant,
                TextNormalizer.containsPattern(search));
        return movementRepository.search(filter, query).map(MovementResultMapper::toResult);
    }
}
