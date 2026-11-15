package com.empresa.gestao.customer.application.port;

import com.empresa.gestao.customer.application.dto.CustomerInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CustomerQueryPort {

    Optional<CustomerInfo> findActive(UUID storeId, UUID id);

    List<UUID> findIdsByName(UUID storeId, String term);

    Map<UUID, String> findNamesByIds(Collection<UUID> ids);
}
