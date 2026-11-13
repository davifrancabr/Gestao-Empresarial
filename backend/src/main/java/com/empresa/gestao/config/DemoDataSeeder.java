package com.empresa.gestao.config;

import com.empresa.gestao.customer.CustomerService;
import com.empresa.gestao.customer.dto.CustomerRequest;
import com.empresa.gestao.movement.MovementService;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.movement.dto.MovementRequest;
import com.empresa.gestao.product.ProductService;
import com.empresa.gestao.product.dto.ProductCreateRequest;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.sale.SaleService;
import com.empresa.gestao.sale.dto.SaleCreateRequest;
import com.empresa.gestao.sale.dto.SaleItemRequest;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import com.empresa.gestao.user.Role;
import com.empresa.gestao.user.User;
import com.empresa.gestao.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Carga inicial de demonstração. Executa apenas quando a tabela de usuários
 * está vazia e APP_SEED_DEMO não é false. Todos os dados são persistidos pela
 * mesma camada de serviço da aplicação, preservando as regras de estoque.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final UUID STORE_ONE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID STORE_TWO = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductService productService;
    private final MovementService movementService;
    private final CustomerService customerService;
    private final SaleService saleService;

    @Value("${app.seed.password:gestao123}")
    private String seedPassword;

    public DemoDataSeeder(UserRepository userRepository, StoreRepository storeRepository,
                          PasswordEncoder passwordEncoder, ProductService productService,
                          MovementService movementService, CustomerService customerService,
                          SaleService saleService) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
        this.productService = productService;
        this.movementService = movementService;
        this.customerService = customerService;
        this.saleService = saleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Carga de demonstração ignorada: usuários já existem.");
            return;
        }
        Store storeOne = storeRepository.findById(STORE_ONE).orElse(null);
        Store storeTwo = storeRepository.findById(STORE_TWO).orElse(null);
        if (storeOne == null || storeTwo == null) {
            log.warn("Carga de demonstração ignorada: lojas iniciais não encontradas.");
            return;
        }

        User admin = createUser("Administrador", "admin@gestao.local", Role.ADMIN, storeOne, storeTwo);
        User manager = createUser("Marina Gerente", "gerente@gestao.local", Role.GERENTE, storeOne);
        createUser("Carlos Operador", "operador@gestao.local", Role.OPERADOR, storeOne);

        seedStoreOne(admin.getId());
        seedStoreTwo(admin.getId());

        var ana = customerService.create(STORE_ONE, new CustomerRequest(
                "Ana Souza", "529.982.247-25", "(11) 98888-1111", "ana.souza@email.com", true));
        customerService.create(STORE_ONE, new CustomerRequest(
                "Bruno Lima", null, "(11) 97777-2222", "bruno.lima@email.com", true));
        customerService.create(STORE_ONE, new CustomerRequest(
                "Carla Mendes", "111.444.777-35", "(11) 96666-3333", "carla.mendes@email.com", true));

        List<ProductResponse> storeOneProducts = productService
                .list(STORE_ONE, null, null, null, "name,asc", 0, 100, false).content();
        ProductResponse rice = find(storeOneProducts, "Arroz 5 kg");
        ProductResponse sugar = find(storeOneProducts, "Açúcar 1 kg");
        if (rice != null && sugar != null) {
            saleService.create(STORE_ONE, manager.getId(), new SaleCreateRequest(ana.id(), List.of(
                    new SaleItemRequest(rice.id(), 2, new BigDecimal("32.90")),
                    new SaleItemRequest(sugar.id(), 3, new BigDecimal("5.49"))
            )));
        }

        log.info("Carga de demonstração criada. Usuários: admin@gestao.local, gerente@gestao.local, "
                + "operador@gestao.local (senha configurada em APP_SEED_PASSWORD).");
    }

    private User createUser(String name, String email, Role role, Store... stores) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        user.setRole(role);
        user.setActive(true);
        for (Store store : stores) {
            user.addStore(store);
        }
        return userRepository.save(user);
    }

    private void seedStoreOne(UUID userId) {
        ProductResponse oil = createProduct(STORE_ONE, new ProductCreateRequest(
                "Óleo de soja 900 ml", "OLEO-900", "Mercearia", "Óleo de soja refinado.", 0, 10, "un"));
        ProductResponse beans = createProduct(STORE_ONE, new ProductCreateRequest(
                "Feijão 1 kg", "FEIJAO-1KG", "Mercearia", "Feijão carioca tipo 1.", 8, 15, "un"));
        ProductResponse milk = createProduct(STORE_ONE, new ProductCreateRequest(
                "Leite integral 1 L", "LEITE-1L", "Laticínios", "Leite integral longa vida.", 14, 24, "un"));
        ProductResponse rice = createProduct(STORE_ONE, new ProductCreateRequest(
                "Arroz 5 kg", "ARROZ-5KG", "Mercearia", "Arroz branco tipo 1.", 42, 20, "un"));
        ProductResponse sugar = createProduct(STORE_ONE, new ProductCreateRequest(
                "Açúcar 1 kg", "ACUCAR-1KG", "Mercearia", "Açúcar refinado.", 25, 12, "un"));
        ProductResponse coffee = createProduct(STORE_ONE, new ProductCreateRequest(
                "Café 500 g", "CAFE-500", "Mercearia", "Café torrado e moído.", 30, 10, "un"));
        ProductResponse soap = createProduct(STORE_ONE, new ProductCreateRequest(
                "Sabão em pó 1 kg", "SABAO-1KG", "Limpeza", "Sabão em pó para roupas.", 6, 8, "un"));
        createProduct(STORE_ONE, new ProductCreateRequest(
                "Refrigerante 2 L", "REFRIG-2L", "Bebidas", "Refrigerante de cola.", 0, 12, "un"));

        Instant now = Instant.now();
        record(userId, oil, MovementType.ENTRY, 12, "Compra de fornecedor",
                "NF 1024", now.minus(6, ChronoUnit.DAYS));
        record(userId, oil, MovementType.EXIT, 12, "Venda", "Saída para consumo", now.minus(2, ChronoUnit.DAYS));
        record(userId, rice, MovementType.ENTRY, 20, "Compra de fornecedor",
                "NF 1031", now.minus(6, ChronoUnit.DAYS));
        record(userId, rice, MovementType.EXIT, 8, "Venda", "Pedido balcão", now.minus(3, ChronoUnit.DAYS));
        record(userId, coffee, MovementType.ENTRY, 10, "Compra de fornecedor",
                "NF 1040", now.minus(5, ChronoUnit.DAYS));
        record(userId, milk, MovementType.EXIT, 6, "Venda", "Pedido balcão", now.minus(1, ChronoUnit.DAYS));
        record(userId, beans, MovementType.ENTRY, 5, "Reposição",
                "Ajuste de gôndola", now.minus(1, ChronoUnit.DAYS));
        record(userId, sugar, MovementType.ADJUSTMENT, 27, "Inventário",
                "Contagem cíclica do estoque", now.minus(4, ChronoUnit.DAYS));
    }

    private void seedStoreTwo(UUID userId) {
        createProduct(STORE_TWO, new ProductCreateRequest(
                "Arroz 5 kg", "ARROZ-5KG", "Mercearia", "Arroz branco tipo 1.", 20, 15, "un"));
        ProductResponse milk = createProduct(STORE_TWO, new ProductCreateRequest(
                "Leite integral 1 L", "LEITE-1L", "Laticínios", "Leite integral longa vida.", 5, 10, "un"));
        record(userId, milk, MovementType.ENTRY, 10, "Compra de fornecedor", "NF 2010",
                Instant.now().minus(2, ChronoUnit.DAYS));
    }

    private ProductResponse createProduct(UUID storeId, ProductCreateRequest request) {
        return productService.create(storeId, adminId(storeId), request);
    }

    private UUID adminId(UUID storeId) {
        return userRepository.findWithStoresByEmail("admin@gestao.local")
                .map(User::getId)
                .orElseThrow();
    }

    private void record(UUID userId, ProductResponse product, MovementType type, int quantity,
                        String reason, String observation, Instant when) {
        movementService.recordAt(product.storeId(), userId, true,
                new MovementRequest(product.id(), type, quantity, reason, observation), when);
    }

    private ProductResponse find(List<ProductResponse> products, String name) {
        return products.stream().filter(product -> product.name().equals(name)).findFirst().orElse(null);
    }
}
