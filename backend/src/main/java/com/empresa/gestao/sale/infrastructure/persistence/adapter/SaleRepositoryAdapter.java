package com.empresa.gestao.sale.infrastructure.persistence.adapter;

import com.empresa.gestao.customer.application.port.CustomerQueryPort;
import com.empresa.gestao.sale.domain.repository.SaleFilter;
import com.empresa.gestao.sale.domain.model.Sale;
import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.sale.infrastructure.persistence.entity.SaleJpaEntity;
import com.empresa.gestao.sale.infrastructure.persistence.mapper.SalePersistenceMapper;
import com.empresa.gestao.sale.infrastructure.persistence.repository.SaleJpaRepository;
import com.empresa.gestao.sale.infrastructure.persistence.repository.SaleSpecifications;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.SaleStatus;
import com.empresa.gestao.shared.infrastructure.persistence.JpaPageMapper;
import com.empresa.gestao.shared.infrastructure.persistence.PageableFactory;
import com.empresa.gestao.user.application.port.UserQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SaleRepositoryAdapter implements SaleRepository {

    private final SaleJpaRepository repository;
    private final CustomerQueryPort customerQueryPort;
    private final UserQueryPort userQueryPort;

    public SaleRepositoryAdapter(SaleJpaRepository repository, CustomerQueryPort customerQueryPort,
                                 UserQueryPort userQueryPort) {
        this.repository = repository;
        this.customerQueryPort = customerQueryPort;
        this.userQueryPort = userQueryPort;
    }

    @Override
    public Sale save(Sale sale) {
        SaleJpaEntity saved = repository.save(SalePersistenceMapper.toEntity(sale));
        return SalePersistenceMapper.toDomain(saved, sale.customerName(), sale.userName());
    }

    @Override
    public Optional<Sale> findDetailByIdAndStoreId(UUID id, UUID storeId) {
        return repository.findDetailByIdAndStoreId(id, storeId)
                .map(entity -> toDomains(List.of(entity)).getFirst());
    }

    @Override
    public PageResult<Sale> search(SaleFilter filter, PageQuery pageQuery) {
        List<UUID> customerIds = null;
        if (filter.searchTerm() != null) {
            customerIds = customerQueryPort.findIdsByName(filter.storeId(), filter.searchTerm());
            if (customerIds.isEmpty()) {
                return PageResult.empty(pageQuery);
            }
        }
        Page<SaleJpaEntity> page = repository.findAll(
                SaleSpecifications.filtered(filter, customerIds),
                PageableFactory.toPageable(pageQuery));
        Map<UUID, String> customerNames = resolveCustomerNames(page.getContent());
        Map<UUID, String> userNames = resolveUserNames(page.getContent());
        return JpaPageMapper.toPageResult(page,
                entity -> toDomain(entity, customerNames, userNames));
    }

    @Override
    public PageResult<Sale> findByCustomer(UUID storeId, UUID customerId, PageQuery pageQuery) {
        Page<SaleJpaEntity> page = repository.findByCustomer(storeId, customerId,
                PageableFactory.toPageable(pageQuery));
        Map<UUID, String> customerNames = resolveCustomerNames(page.getContent());
        Map<UUID, String> userNames = resolveUserNames(page.getContent());
        return JpaPageMapper.toPageResult(page,
                entity -> toDomain(entity, customerNames, userNames));
    }

    @Override
    public long countByStoreId(UUID storeId) {
        return repository.countByStoreId(storeId);
    }

    @Override
    public BigDecimal sumRevenue(UUID storeId, SaleStatus status) {
        return repository.sumRevenue(storeId, status);
    }

    @Override
    public BigDecimal sumRevenue(UUID storeId, SaleStatus status, Instant from, Instant to) {
        return repository.sumRevenue(storeId, status, from, to);
    }

    private List<Sale> toDomains(List<SaleJpaEntity> entities) {
        Map<UUID, String> customerNames = resolveCustomerNames(entities);
        Map<UUID, String> userNames = resolveUserNames(entities);
        return entities.stream().map(entity -> toDomain(entity, customerNames, userNames)).toList();
    }

    private Sale toDomain(SaleJpaEntity entity, Map<UUID, String> customerNames,
                          Map<UUID, String> userNames) {
        return SalePersistenceMapper.toDomain(entity,
                entity.getCustomerId() == null ? null : customerNames.get(entity.getCustomerId()),
                userNames.get(entity.getUserId()));
    }

    private Map<UUID, String> resolveCustomerNames(List<SaleJpaEntity> entities) {
        List<UUID> ids = new ArrayList<>();
        for (SaleJpaEntity entity : entities) {
            if (entity.getCustomerId() != null && !ids.contains(entity.getCustomerId())) {
                ids.add(entity.getCustomerId());
            }
        }
        return customerQueryPort.findNamesByIds(ids);
    }

    private Map<UUID, String> resolveUserNames(List<SaleJpaEntity> entities) {
        List<UUID> ids = new ArrayList<>();
        for (SaleJpaEntity entity : entities) {
            if (!ids.contains(entity.getUserId())) {
                ids.add(entity.getUserId());
            }
        }
        return userQueryPort.findNamesByIds(ids);
    }
}
