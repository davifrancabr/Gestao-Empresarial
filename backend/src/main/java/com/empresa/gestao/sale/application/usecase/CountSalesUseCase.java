package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.sale.domain.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CountSalesUseCase {

    private final SaleRepository saleRepository;

    public CountSalesUseCase(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public long execute(UUID storeId) {
        return saleRepository.countByStoreId(storeId);
    }
}
