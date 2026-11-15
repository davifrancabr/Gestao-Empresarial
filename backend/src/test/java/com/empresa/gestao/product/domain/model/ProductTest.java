package com.empresa.gestao.product.domain.model;

import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.model.ProductStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void entryIncreasesBalanceAndRecordsRequestedQuantity() {
        StockChange change = product(5).registerEntry(7);

        assertThat(change.previousQuantity()).isEqualTo(5);
        assertThat(change.resultingQuantity()).isEqualTo(12);
        assertThat(change.recordedQuantity()).isEqualTo(7);
    }

    @Test
    void exitDecreasesBalance() {
        StockChange change = product(5).registerExit(3);

        assertThat(change.resultingQuantity()).isEqualTo(2);
        assertThat(change.recordedQuantity()).isEqualTo(3);
    }

    @Test
    void exitCannotExceedAvailableBalance() {
        assertThatThrownBy(() -> product(1).registerExit(2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saída indisponível. Há apenas 1 unidade em estoque.");
        assertThatThrownBy(() -> product(5).registerExit(6))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saída indisponível. Há apenas 5 unidades em estoque.");
    }

    @Test
    void entryAndExitRequirePositiveQuantity() {
        assertThatThrownBy(() -> product(5).registerEntry(0))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Informe uma quantidade inteira maior que zero.");
        assertThatThrownBy(() -> product(5).registerExit(-1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Informe uma quantidade inteira maior que zero.");
    }

    @Test
    void adjustmentSetsTargetBalanceAndRecordsAbsoluteDifference() {
        StockChange decrease = product(5).adjustTo(3);
        StockChange increase = product(5).adjustTo(9);

        assertThat(decrease.resultingQuantity()).isEqualTo(3);
        assertThat(decrease.recordedQuantity()).isEqualTo(2);
        assertThat(increase.resultingQuantity()).isEqualTo(9);
        assertThat(increase.recordedQuantity()).isEqualTo(4);
    }

    @Test
    void adjustmentRejectsNegativeAndUnchangedBalance() {
        assertThatThrownBy(() -> product(5).adjustTo(-1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O saldo ajustado não pode ser negativo.");
        assertThatThrownBy(() -> product(5).adjustTo(5))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O saldo informado é igual ao saldo atual.");
    }

    @Test
    void statusFollowsMinimumBalance() {
        assertThat(product(10, 5).status()).isEqualTo(ProductStatus.NORMAL);
        assertThat(product(5, 5).status()).isEqualTo(ProductStatus.LOW);
        assertThat(product(0, 5).status()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    private Product product(int quantity) {
        return product(quantity, 2);
    }

    private Product product(int quantity, int minimum) {
        return new Product(UUID.randomUUID(), UUID.randomUUID(), "Arroz 5 kg", "arroz 5 kg",
                "ARROZ-5KG", "Mercearia", "Arroz branco tipo 1.", quantity, minimum, "un",
                true, 0L, null, null);
    }
}
