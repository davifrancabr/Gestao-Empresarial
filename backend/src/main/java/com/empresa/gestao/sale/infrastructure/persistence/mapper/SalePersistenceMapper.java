package com.empresa.gestao.sale.infrastructure.persistence.mapper;

import com.empresa.gestao.sale.domain.model.Sale;
import com.empresa.gestao.sale.domain.model.SaleItem;
import com.empresa.gestao.sale.infrastructure.persistence.entity.SaleItemJpaEntity;
import com.empresa.gestao.sale.infrastructure.persistence.entity.SaleJpaEntity;

import java.util.List;

public final class SalePersistenceMapper {

    private SalePersistenceMapper() {
    }

    public static Sale toDomain(SaleJpaEntity entity, String customerName, String userName) {
        List<SaleItem> items = entity.getItems().stream()
                .map(item -> new SaleItem(item.getId(), item.getProductId(),
                        item.getProductNameSnapshot(), item.getQuantity(), item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        return new Sale(entity.getId(), entity.getStoreId(), entity.getCustomerId(), customerName,
                entity.getUserId(), userName, entity.getTotal(), entity.getStatus(),
                entity.getCreatedAt(), items);
    }

    public static SaleJpaEntity toEntity(Sale sale) {
        SaleJpaEntity entity = new SaleJpaEntity();
        entity.setStoreId(sale.storeId());
        entity.setCustomerId(sale.customerId());
        entity.setUserId(sale.userId());
        entity.setTotal(sale.total());
        entity.setStatus(sale.status());
        for (SaleItem item : sale.items()) {
            SaleItemJpaEntity itemEntity = new SaleItemJpaEntity();
            itemEntity.setProductId(item.productId());
            itemEntity.setProductNameSnapshot(item.productNameSnapshot());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setUnitPrice(item.unitPrice());
            itemEntity.setSubtotal(item.subtotal());
            entity.addItem(itemEntity);
        }
        return entity;
    }
}
