package com.empresa.gestao.product.application.port;

import com.empresa.gestao.product.application.dto.ProductInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductQueryPort {

    Optional<ProductInfo> findActive(UUID storeId, UUID id);

    List<UUID> findIdsByNameOrSku(UUID storeId, String term);

    Map<UUID, String> findSkusByIds(Collection<UUID> ids);
}
