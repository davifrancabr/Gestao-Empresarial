package com.empresa.gestao.sale.domain.repository;

import com.empresa.gestao.sale.domain.repository.SaleFilter;
import com.empresa.gestao.sale.domain.model.Sale;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository {

    Sale save(Sale sale);

    Optional<Sale> findDetailByIdAndStoreId(UUID id, UUID storeId);

    PageResult<Sale> search(SaleFilter filter, PageQuery pageQuery);

    PageResult<Sale> findByCustomer(UUID storeId, UUID customerId, PageQuery pageQuery);

    long countByStoreId(UUID storeId);

    BigDecimal sumRevenue(UUID storeId, SaleStatus status);

    BigDecimal sumRevenue(UUID storeId, SaleStatus status, Instant from, Instant to);
}
