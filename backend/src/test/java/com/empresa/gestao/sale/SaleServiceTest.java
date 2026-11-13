package com.empresa.gestao.sale;

import com.empresa.gestao.common.exception.BusinessRuleException;
import com.empresa.gestao.customer.CustomerService;
import com.empresa.gestao.movement.MovementService;
import com.empresa.gestao.product.Product;
import com.empresa.gestao.product.ProductRepository;
import com.empresa.gestao.sale.dto.SaleCreateRequest;
import com.empresa.gestao.sale.dto.SaleItemRequest;
import com.empresa.gestao.sale.dto.SaleResponse;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import com.empresa.gestao.user.User;
import com.empresa.gestao.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private MovementService movementService;

    private SaleService saleService;
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private Store store;
    private User user;
    private Product rice;
    private Product sugar;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(saleRepository, productRepository, storeRepository,
                userRepository, customerService, movementService, new SaleMapper());
        store = new Store();
        store.setId(storeId);
        store.setName("Loja");
        user = new User();
        user.setId(userId);
        user.setName("Gerente");
        rice = product("Arroz 5 kg", 10);
        sugar = product("Açúcar 1 kg", 8);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        org.mockito.Mockito.lenient().when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            sale.setId(UUID.randomUUID());
            return sale;
        });
    }

    @Test
    void createsSaleAndRegistersExitForEachItem() {
        when(productRepository.findByIdAndStoreIdAndActiveTrue(rice.getId(), storeId))
                .thenReturn(Optional.of(rice));
        when(productRepository.findByIdAndStoreIdAndActiveTrue(sugar.getId(), storeId))
                .thenReturn(Optional.of(sugar));

        SaleResponse response = saleService.create(storeId, userId, new SaleCreateRequest(null, List.of(
                new SaleItemRequest(rice.getId(), 2, new BigDecimal("32.90")),
                new SaleItemRequest(sugar.getId(), 3, new BigDecimal("5.49"))
        )));

        assertThat(response.total()).isEqualByComparingTo("82.27");
        assertThat(response.items()).hasSize(2);
        verify(movementService, times(2)).apply(eq(store), eq(user), any(Product.class), eq(false), any(), any());
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void rejectsSaleAboveAvailableStock() {
        when(productRepository.findByIdAndStoreIdAndActiveTrue(rice.getId(), storeId))
                .thenReturn(Optional.of(rice));

        assertThatThrownBy(() -> saleService.create(storeId, userId, new SaleCreateRequest(null, List.of(
                new SaleItemRequest(rice.getId(), 11, BigDecimal.TEN)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(saleRepository, never()).save(any());
        verify(movementService, never()).apply(any(), any(), any(), eq(false), any(), any());
    }

    @Test
    void rejectsRepeatedProductInSameSale() {
        when(productRepository.findByIdAndStoreIdAndActiveTrue(rice.getId(), storeId))
                .thenReturn(Optional.of(rice));

        assertThatThrownBy(() -> saleService.create(storeId, userId, new SaleCreateRequest(null, List.of(
                new SaleItemRequest(rice.getId(), 1, BigDecimal.ONE),
                new SaleItemRequest(rice.getId(), 1, BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mesmo produto");

        verify(saleRepository, never()).save(any());
    }

    private Product product(String name, int quantity) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setStore(store);
        product.setName(name);
        product.setNormalizedName(name.toLowerCase());
        product.setSku(name.substring(0, 3).toUpperCase());
        product.setQuantity(quantity);
        product.setMinimum(1);
        product.setUnit("un");
        product.setActive(true);
        return product;
    }
}
