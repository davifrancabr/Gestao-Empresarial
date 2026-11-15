package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.customer.application.port.CustomerQueryPort;
import com.empresa.gestao.movement.application.usecase.RecordMovementUseCase;
import com.empresa.gestao.product.application.dto.ProductInfo;
import com.empresa.gestao.product.application.port.ProductQueryPort;
import com.empresa.gestao.sale.application.dto.SaleCreateCommand;
import com.empresa.gestao.sale.application.dto.SaleItemCommand;
import com.empresa.gestao.sale.application.dto.SaleResult;
import com.empresa.gestao.sale.domain.model.Sale;
import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.model.Role;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserInfo;
import com.empresa.gestao.user.application.port.UserQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSaleUseCaseTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private ProductQueryPort productQueryPort;
    @Mock
    private CustomerQueryPort customerQueryPort;
    @Mock
    private UserQueryPort userQueryPort;
    @Mock
    private StoreQueryPort storeQueryPort;
    @Mock
    private RecordMovementUseCase recordMovement;

    private CreateSaleUseCase useCase;
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID riceId = UUID.randomUUID();
    private final UUID sugarId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CreateSaleUseCase(saleRepository, productQueryPort, customerQueryPort,
                userQueryPort, storeQueryPort, recordMovement);
        when(storeQueryPort.getRequired(storeId))
                .thenReturn(new StoreResult(storeId, "Loja", null, true, null, null));
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(
                new UserInfo(userId, "Gerente", "gerente@gestao.local", Role.GERENTE, true)));
    }

    @Test
    void createsSaleAndRegistersExitForEachItem() {
        stubSave();
        when(productQueryPort.findActive(storeId, riceId))
                .thenReturn(Optional.of(product(riceId, "Arroz 5 kg", 10)));
        when(productQueryPort.findActive(storeId, sugarId))
                .thenReturn(Optional.of(product(sugarId, "Açúcar 1 kg", 8)));

        SaleResult result = useCase.execute(storeId, userId, new SaleCreateCommand(null, List.of(
                new SaleItemCommand(riceId, 2, new BigDecimal("32.90")),
                new SaleItemCommand(sugarId, 3, new BigDecimal("5.49"))
        )));

        assertThat(result.total()).isEqualByComparingTo("82.27");
        assertThat(result.items()).hasSize(2);
        assertThat(result.customerName()).isEqualTo("Consumidor final");
        assertThat(result.status().name()).isEqualTo("CONCLUIDA");
        verify(recordMovement, times(2)).record(any(), any(), any(Boolean.class), any());
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void rejectsSaleAboveAvailableStock() {
        when(productQueryPort.findActive(storeId, riceId))
                .thenReturn(Optional.of(product(riceId, "Arroz 5 kg", 10)));

        assertThatThrownBy(() -> useCase.execute(storeId, userId, new SaleCreateCommand(null, List.of(
                new SaleItemCommand(riceId, 11, BigDecimal.TEN)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(saleRepository, never()).save(any());
        verify(recordMovement, never()).record(any(), any(), any(Boolean.class), any());
    }

    @Test
    void rejectsRepeatedProductInSameSale() {
        when(productQueryPort.findActive(storeId, riceId))
                .thenReturn(Optional.of(product(riceId, "Arroz 5 kg", 10)));

        assertThatThrownBy(() -> useCase.execute(storeId, userId, new SaleCreateCommand(null, List.of(
                new SaleItemCommand(riceId, 1, BigDecimal.ONE),
                new SaleItemCommand(riceId, 1, BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mesmo produto");

        verify(saleRepository, never()).save(any());
        verify(recordMovement, never()).record(any(), any(), any(Boolean.class), any());
    }

    @Test
    void saleWithoutCustomerDoesNotQueryCustomers() {
        stubSave();
        when(productQueryPort.findActive(storeId, riceId))
                .thenReturn(Optional.of(product(riceId, "Arroz 5 kg", 10)));

        useCase.execute(storeId, userId, new SaleCreateCommand(null, List.of(
                new SaleItemCommand(riceId, 1, BigDecimal.TEN))));

        verify(customerQueryPort, never()).findActive(any(), any());
    }

    private void stubSave() {
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            return new Sale(UUID.randomUUID(), sale.storeId(), sale.customerId(), sale.customerName(),
                    sale.userId(), sale.userName(), sale.total(), sale.status(), Instant.now(),
                    sale.items());
        });
    }

    private ProductInfo product(UUID id, String name, int quantity) {
        return new ProductInfo(id, storeId, name, name.substring(0, 3).toUpperCase(), quantity,
                1, "un", true, 0L);
    }
}
