package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.customer.application.dto.CustomerInfo;
import com.empresa.gestao.customer.application.port.CustomerQueryPort;
import com.empresa.gestao.movement.application.dto.MovementCommand;
import com.empresa.gestao.movement.application.usecase.RecordMovementUseCase;
import com.empresa.gestao.product.application.dto.ProductInfo;
import com.empresa.gestao.product.application.port.ProductQueryPort;
import com.empresa.gestao.sale.application.dto.SaleCreateCommand;
import com.empresa.gestao.sale.application.dto.SaleItemCommand;
import com.empresa.gestao.sale.application.dto.SaleResult;
import com.empresa.gestao.sale.domain.model.Sale;
import com.empresa.gestao.sale.domain.model.SaleItem;
import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.domain.model.SaleStatus;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserInfo;
import com.empresa.gestao.user.application.port.UserQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateSaleUseCase {

    private final SaleRepository saleRepository;
    private final ProductQueryPort productQueryPort;
    private final CustomerQueryPort customerQueryPort;
    private final UserQueryPort userQueryPort;
    private final StoreQueryPort storeQueryPort;
    private final RecordMovementUseCase recordMovement;

    public CreateSaleUseCase(SaleRepository saleRepository, ProductQueryPort productQueryPort,
                             CustomerQueryPort customerQueryPort, UserQueryPort userQueryPort,
                             StoreQueryPort storeQueryPort, RecordMovementUseCase recordMovement) {
        this.saleRepository = saleRepository;
        this.productQueryPort = productQueryPort;
        this.customerQueryPort = customerQueryPort;
        this.userQueryPort = userQueryPort;
        this.storeQueryPort = storeQueryPort;
        this.recordMovement = recordMovement;
    }

    @Transactional
    public SaleResult execute(UUID storeId, UUID userId, SaleCreateCommand command) {
        storeQueryPort.getRequired(storeId);
        UserInfo user = userQueryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        CustomerInfo customer = command.customerId() == null ? null
                : customerQueryPort.findActive(storeId, command.customerId())
                        .orElseThrow(() -> new NotFoundException("Cliente não encontrado ou arquivado."));

        Set<UUID> seenProducts = new HashSet<>();
        List<ProductInfo> products = new ArrayList<>();
        List<SaleItem> saleItems = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemCommand item : command.items()) {
            if (!seenProducts.add(item.productId())) {
                throw new BusinessRuleException("O mesmo produto foi informado mais de uma vez na venda.");
            }
            ProductInfo product = productQueryPort.findActive(storeId, item.productId())
                    .orElseThrow(() -> new NotFoundException("Produto não encontrado ou arquivado."));
            if (item.quantity() > product.quantity()) {
                throw new BusinessRuleException(
                        "Estoque insuficiente para " + product.name() + ". Disponível: "
                                + product.quantity() + ".");
            }

            BigDecimal subtotal = null;
            if (item.unitPrice() != null) {
                subtotal = item.unitPrice()
                        .multiply(BigDecimal.valueOf(item.quantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                total = total.add(subtotal);
            }

            saleItems.add(new SaleItem(null, product.id(), product.name(), item.quantity(),
                    item.unitPrice(), subtotal));
            products.add(product);
        }

        Sale sale = new Sale(null, storeId,
                customer == null ? null : customer.id(),
                customer == null ? null : customer.name(),
                user.id(), user.name(), total.setScale(2, RoundingMode.HALF_UP),
                SaleStatus.CONCLUIDA, null, saleItems);
        Sale saved = saleRepository.save(sale);

        for (int index = 0; index < products.size(); index++) {
            ProductInfo product = products.get(index);
            SaleItemCommand item = command.items().get(index);
            recordMovement.record(storeId, userId, false, new MovementCommand(
                    product.id(),
                    MovementType.EXIT,
                    item.quantity(),
                    "Venda",
                    "Venda " + saved.id()
            ));
        }

        return SaleResultMapper.toResult(saved);
    }
}
