package com.empresa.gestao.bootstrap.seed;

import com.empresa.gestao.customer.application.dto.CustomerCommand;
import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.application.usecase.CreateCustomerUseCase;
import com.empresa.gestao.movement.application.dto.MovementCommand;
import com.empresa.gestao.movement.application.usecase.RecordMovementUseCase;
import com.empresa.gestao.product.application.dto.ProductCreateCommand;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.usecase.CreateProductUseCase;
import com.empresa.gestao.product.application.usecase.ListProductsUseCase;
import com.empresa.gestao.sale.application.dto.SaleCreateCommand;
import com.empresa.gestao.sale.application.dto.SaleItemCommand;
import com.empresa.gestao.sale.application.usecase.CreateSaleUseCase;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.domain.model.Role;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserCreateCommand;
import com.empresa.gestao.user.application.dto.UserResult;
import com.empresa.gestao.user.application.usecase.CountUsersUseCase;
import com.empresa.gestao.user.application.usecase.CreateUserUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Carga inicial de demonstração. Executa apenas quando a tabela de usuários
 * está vazia e {@code app.seed.demo} não é false. Todos os dados são
 * persistidos pelos casos de uso, preservando as regras de estoque e venda.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final UUID STORE_ONE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID STORE_TWO = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private final CountUsersUseCase countUsers;
    private final CreateUserUseCase createUserUseCase;
    private final CreateProductUseCase createProduct;
    private final ListProductsUseCase listProducts;
    private final RecordMovementUseCase recordMovement;
    private final CreateCustomerUseCase createCustomer;
    private final CreateSaleUseCase createSale;
    private final StoreQueryPort storeQueryPort;

    private final String seedPassword;
    private UUID adminId;

    public DemoDataSeeder(CountUsersUseCase countUsers, CreateUserUseCase createUserUseCase,
                          CreateProductUseCase createProduct, ListProductsUseCase listProducts,
                          RecordMovementUseCase recordMovement, CreateCustomerUseCase createCustomer,
                          CreateSaleUseCase createSale, StoreQueryPort storeQueryPort,
                          @Value("${app.seed.password:gestao123}") String seedPassword) {
        this.countUsers = countUsers;
        this.createUserUseCase = createUserUseCase;
        this.createProduct = createProduct;
        this.listProducts = listProducts;
        this.recordMovement = recordMovement;
        this.createCustomer = createCustomer;
        this.createSale = createSale;
        this.storeQueryPort = storeQueryPort;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (countUsers.execute() > 0) {
            log.info("Carga de demonstração ignorada: usuários já existem.");
            return;
        }
        if (storeQueryPort.findStoreById(STORE_ONE).isEmpty()
                || storeQueryPort.findStoreById(STORE_TWO).isEmpty()) {
            log.warn("Carga de demonstração ignorada: lojas iniciais não encontradas.");
            return;
        }

        UserResult admin = seedUser("Administrador", "admin@gestao.local", Role.ADMIN,
                List.of(STORE_ONE, STORE_TWO));
        adminId = admin.id();
        UserResult manager = seedUser("Marina Gerente", "gerente@gestao.local", Role.GERENTE,
                List.of(STORE_ONE));
        seedUser("Carlos Operador", "operador@gestao.local", Role.OPERADOR, List.of(STORE_ONE));

        seedStoreOne(admin.id());
        seedStoreTwo(admin.id());

        CustomerResult ana = createCustomer.execute(STORE_ONE, new CustomerCommand(
                "Ana Souza", "529.982.247-25", "(11) 98888-1111", "ana.souza@email.com", true));
        createCustomer.execute(STORE_ONE, new CustomerCommand(
                "Bruno Lima", null, "(11) 97777-2222", "bruno.lima@email.com", true));
        createCustomer.execute(STORE_ONE, new CustomerCommand(
                "Carla Mendes", "111.444.777-35", "(11) 96666-3333", "carla.mendes@email.com", true));

        List<ProductResult> storeOneProducts = listProducts.execute(STORE_ONE, null, null, null,
                "name,asc", 0, 100, false).items();
        ProductResult rice = find(storeOneProducts, "Arroz 5 kg");
        ProductResult sugar = find(storeOneProducts, "Açúcar 1 kg");
        if (rice != null && sugar != null) {
            createSale.execute(STORE_ONE, manager.id(), new SaleCreateCommand(ana.id(), List.of(
                    new SaleItemCommand(rice.id(), 2, new BigDecimal("32.90")),
                    new SaleItemCommand(sugar.id(), 3, new BigDecimal("5.49"))
            )));
        }

        log.info("Carga de demonstração criada. Usuários: admin@gestao.local, gerente@gestao.local, "
                + "operador@gestao.local (senha configurada em APP_SEED_PASSWORD).");
    }

    private UserResult seedUser(String name, String email, Role role, List<UUID> storeIds) {
        return createUserUseCase.execute(new UserCreateCommand(name, email, seedPassword, role, storeIds));
    }

    private void seedStoreOne(UUID userId) {
        ProductResult oil = product(STORE_ONE, new ProductCreateCommand(
                "Óleo de soja 900 ml", "OLEO-900", "Mercearia", "Óleo de soja refinado.", 0, 10, "un"));
        ProductResult beans = product(STORE_ONE, new ProductCreateCommand(
                "Feijão 1 kg", "FEIJAO-1KG", "Mercearia", "Feijão carioca tipo 1.", 8, 15, "un"));
        ProductResult milk = product(STORE_ONE, new ProductCreateCommand(
                "Leite integral 1 L", "LEITE-1L", "Laticínios", "Leite integral longa vida.", 14, 24, "un"));
        ProductResult rice = product(STORE_ONE, new ProductCreateCommand(
                "Arroz 5 kg", "ARROZ-5KG", "Mercearia", "Arroz branco tipo 1.", 42, 20, "un"));
        ProductResult sugar = product(STORE_ONE, new ProductCreateCommand(
                "Açúcar 1 kg", "ACUCAR-1KG", "Mercearia", "Açúcar refinado.", 25, 12, "un"));
        ProductResult coffee = product(STORE_ONE, new ProductCreateCommand(
                "Café 500 g", "CAFE-500", "Mercearia", "Café torrado e moído.", 30, 10, "un"));
        ProductResult soap = product(STORE_ONE, new ProductCreateCommand(
                "Sabão em pó 1 kg", "SABAO-1KG", "Limpeza", "Sabão em pó para roupas.", 6, 8, "un"));
        product(STORE_ONE, new ProductCreateCommand(
                "Refrigerante 2 L", "REFRIG-2L", "Bebidas", "Refrigerante de cola.", 0, 12, "un"));

        Instant now = Instant.now();
        record(userId, oil, MovementType.ENTRY, 12, "Compra de fornecedor",
                "NF 1024", now.minus(6, ChronoUnit.DAYS));
        record(userId, oil, MovementType.EXIT, 12, "Venda", "Saída para consumo",
                now.minus(2, ChronoUnit.DAYS));
        record(userId, rice, MovementType.ENTRY, 20, "Compra de fornecedor",
                "NF 1031", now.minus(6, ChronoUnit.DAYS));
        record(userId, rice, MovementType.EXIT, 8, "Venda", "Pedido balcão",
                now.minus(3, ChronoUnit.DAYS));
        record(userId, coffee, MovementType.ENTRY, 10, "Compra de fornecedor",
                "NF 1040", now.minus(5, ChronoUnit.DAYS));
        record(userId, milk, MovementType.EXIT, 6, "Venda", "Pedido balcão",
                now.minus(1, ChronoUnit.DAYS));
        record(userId, beans, MovementType.ENTRY, 5, "Reposição",
                "Ajuste de gôndola", now.minus(1, ChronoUnit.DAYS));
        record(userId, sugar, MovementType.ADJUSTMENT, 27, "Inventário",
                "Contagem cíclica do estoque", now.minus(4, ChronoUnit.DAYS));
    }

    private void seedStoreTwo(UUID userId) {
        product(STORE_TWO, new ProductCreateCommand(
                "Arroz 5 kg", "ARROZ-5KG", "Mercearia", "Arroz branco tipo 1.", 20, 15, "un"));
        ProductResult milk = product(STORE_TWO, new ProductCreateCommand(
                "Leite integral 1 L", "LEITE-1L", "Laticínios", "Leite integral longa vida.", 5, 10, "un"));
        record(userId, milk, MovementType.ENTRY, 10, "Compra de fornecedor", "NF 2010",
                Instant.now().minus(2, ChronoUnit.DAYS));
    }

    private ProductResult product(UUID storeId, ProductCreateCommand command) {
        return createProduct.execute(storeId, adminId, command);
    }

    private void record(UUID userId, ProductResult product, MovementType type, int quantity,
                        String reason, String observation, Instant when) {
        recordMovement.recordAt(product.storeId(), userId, true,
                new MovementCommand(product.id(), type, quantity, reason, observation), when);
    }

    private ProductResult find(List<ProductResult> products, String name) {
        return products.stream().filter(product -> product.name().equals(name)).findFirst().orElse(null);
    }
}
