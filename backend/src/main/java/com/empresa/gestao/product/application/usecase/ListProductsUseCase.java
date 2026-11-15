package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.domain.repository.ProductFilter;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.ProductStatus;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ListProductsUseCase {

    private final ProductRepository productRepository;

    public ListProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResult> execute(UUID storeId, String search, String category,
                                             ProductStatus status, String sort, Integer page,
                                             Integer size, boolean includeInactive) {
        String term = TextNormalizer.containsPattern(search);
        String cleanCategory = TextNormalizer.cleanText(category);
        PageQuery query = PageQuery.of(page, size, sort, Map.of(
                "name", "normalizedName",
                "quantity", "quantity",
                "minimum", "minimum",
                "category", "category",
                "createdAt", "createdAt",
                "updatedAt", "updatedAt"
        ), "name,asc");
        ProductFilter filter = new ProductFilter(storeId, term, cleanCategory, status, includeInactive);
        return productRepository.search(filter, query).map(ProductResultMapper::toResult);
    }
}
