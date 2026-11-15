package com.empresa.gestao.movement.infrastructure.persistence.adapter;

import com.empresa.gestao.movement.domain.repository.DailyMovementCount;
import com.empresa.gestao.movement.domain.repository.MovementFilter;
import com.empresa.gestao.movement.domain.repository.TopMovedProduct;
import com.empresa.gestao.movement.domain.model.StockMovement;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.movement.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.empresa.gestao.movement.infrastructure.persistence.mapper.StockMovementPersistenceMapper;
import com.empresa.gestao.movement.infrastructure.persistence.repository.StockMovementJpaRepository;
import com.empresa.gestao.movement.infrastructure.persistence.repository.StockMovementSpecifications;
import com.empresa.gestao.product.application.port.ProductQueryPort;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.infrastructure.persistence.JpaPageMapper;
import com.empresa.gestao.shared.infrastructure.persistence.PageableFactory;
import com.empresa.gestao.user.application.port.UserQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockMovementRepositoryAdapter implements StockMovementRepository {

    private final StockMovementJpaRepository repository;
    private final ProductQueryPort productQueryPort;
    private final UserQueryPort userQueryPort;

    public StockMovementRepositoryAdapter(StockMovementJpaRepository repository,
                                          ProductQueryPort productQueryPort,
                                          UserQueryPort userQueryPort) {
        this.repository = repository;
        this.productQueryPort = productQueryPort;
        this.userQueryPort = userQueryPort;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        StockMovementJpaEntity saved = repository.save(StockMovementPersistenceMapper.toEntity(movement));
        return StockMovementPersistenceMapper.toDomain(saved, movement.productSku(), movement.userName());
    }

    @Override
    public Optional<StockMovement> findByIdAndStoreId(UUID id, UUID storeId) {
        return repository.findByIdAndStoreId(id, storeId)
                .map(entity -> toDomains(List.of(entity)).getFirst());
    }

    @Override
    public PageResult<StockMovement> search(MovementFilter filter, PageQuery pageQuery) {
        List<UUID> productIds = null;
        if (filter.searchTerm() != null) {
            productIds = productQueryPort.findIdsByNameOrSku(filter.storeId(), filter.searchTerm());
            if (productIds.isEmpty()) {
                return PageResult.empty(pageQuery);
            }
        }
        Page<StockMovementJpaEntity> page = repository.findAll(
                StockMovementSpecifications.filtered(filter, productIds),
                PageableFactory.toPageable(pageQuery));
        Map<UUID, String> skus = resolveSkus(page.getContent());
        Map<UUID, String> names = resolveUserNames(page.getContent());
        return JpaPageMapper.toPageResult(page, entity -> toDomain(entity, skus, names));
    }

    @Override
    public List<StockMovement> findRecent(UUID storeId, int limit) {
        return toDomains(repository.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(0, limit)));
    }

    @Override
    public long countByStoreId(UUID storeId) {
        return repository.countByStoreId(storeId);
    }

    @Override
    public long countByStoreIdAndType(UUID storeId, MovementType type) {
        return repository.countByStoreIdAndType(storeId, type);
    }

    @Override
    public List<TopMovedProduct> findTopMoved(UUID storeId, int limit) {
        return toTopProducts(repository.findTopMoved(storeId, PageRequest.of(0, limit)));
    }

    @Override
    public List<TopMovedProduct> findTopMovedSince(UUID storeId, java.time.Instant since, int limit) {
        return toTopProducts(repository.findTopMovedSince(storeId, since, PageRequest.of(0, limit)));
    }

    @Override
    public List<DailyMovementCount> countDaily(UUID storeId, java.time.Instant since) {
        return repository.countDaily(storeId, since).stream()
                .map(row -> new DailyMovementCount(row.getDay().toLocalDate(),
                        MovementType.valueOf(row.getType()),
                        row.getTotal() == null ? 0 : row.getTotal()))
                .toList();
    }

    private List<TopMovedProduct> toTopProducts(
            List<StockMovementJpaRepository.ProductMovementAggregateRow> rows) {
        return rows.stream()
                .map(row -> new TopMovedProduct(row.getProductId(), row.getProductName(),
                        row.getTotalQuantity() == null ? 0 : row.getTotalQuantity(),
                        row.getMovementCount() == null ? 0 : row.getMovementCount()))
                .toList();
    }

    private List<StockMovement> toDomains(List<StockMovementJpaEntity> entities) {
        Map<UUID, String> skus = resolveSkus(entities);
        Map<UUID, String> names = resolveUserNames(entities);
        return entities.stream().map(entity -> toDomain(entity, skus, names)).toList();
    }

    private StockMovement toDomain(StockMovementJpaEntity entity, Map<UUID, String> skus,
                                   Map<UUID, String> names) {
        return StockMovementPersistenceMapper.toDomain(entity, skus.get(entity.getProductId()),
                names.get(entity.getUserId()));
    }

    private Map<UUID, String> resolveSkus(List<StockMovementJpaEntity> entities) {
        List<UUID> ids = distinct(entities, true);
        return new HashMap<>(productQueryPort.findSkusByIds(ids));
    }

    private Map<UUID, String> resolveUserNames(List<StockMovementJpaEntity> entities) {
        List<UUID> ids = distinct(entities, false);
        return new HashMap<>(userQueryPort.findNamesByIds(ids));
    }

    private List<UUID> distinct(List<StockMovementJpaEntity> entities, boolean product) {
        List<UUID> ids = new ArrayList<>();
        for (StockMovementJpaEntity entity : entities) {
            UUID id = product ? entity.getProductId() : entity.getUserId();
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }
}
