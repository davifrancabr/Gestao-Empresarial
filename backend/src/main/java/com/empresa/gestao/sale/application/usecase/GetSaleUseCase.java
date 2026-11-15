package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.sale.application.dto.SaleResult;
import com.empresa.gestao.sale.domain.model.Sale;
import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetSaleUseCase {

    private final SaleRepository saleRepository;

    public GetSaleUseCase(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public SaleResult execute(UUID storeId, UUID id) {
        Sale sale = saleRepository.findDetailByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException("Venda não encontrada."));
        return SaleResultMapper.toResult(sale);
    }
}
