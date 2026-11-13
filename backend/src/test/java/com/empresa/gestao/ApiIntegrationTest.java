package com.empresa.gestao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("databaseAvailable")
class ApiIntegrationTest {

    private static final String STORE_ONE = "11111111-1111-4111-8111-111111111111";
    private static final String STORE_TWO = "22222222-2222-4222-8222-222222222222";

    /**
     * When GESTAO_TEST_DB_URL (or -Dtest.db.url) is provided the suite runs against
     * that PostgreSQL instance; otherwise a disposable Testcontainers database is
     * started. Tests are skipped when neither is available.
     */
    private static final String EXTERNAL_DB_URL = firstNonBlank(
            System.getProperty("test.db.url"), System.getenv("GESTAO_TEST_DB_URL"));

    private static PostgreSQLContainer<?> postgres;

    static boolean databaseAvailable() {
        return EXTERNAL_DB_URL != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        if (EXTERNAL_DB_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB_URL);
            registry.add("spring.datasource.username", () -> firstNonBlank(
                    System.getProperty("test.db.username"), System.getenv("GESTAO_TEST_DB_USERNAME"), "gestao"));
            registry.add("spring.datasource.password", () -> firstNonBlank(
                    System.getProperty("test.db.password"), System.getenv("GESTAO_TEST_DB_PASSWORD"), "gestao"));
            return;
        }
        postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginReturnsSessionWithUserAndStores() throws Exception {
        MockHttpSession session = login("admin@gestao.local", "gestao123");
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("admin@gestao.local"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.stores.length()").value(2));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@gestao.local", "password", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productAndMovementLifecycleKeepsStockConsistent() throws Exception {
        MockHttpSession session = login("gerente@gestao.local", "gestao123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mockMvc.perform(post("/api/v1/products").with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Produto Teste " + suffix,
                                "sku", "TEST-" + suffix,
                                "category", "Testes",
                                "description", "Produto criado pelo teste de integração",
                                "quantity", 5,
                                "minimum", 2,
                                "unit", "un"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.status").value("NORMAL"))
                .andReturn();

        JsonNode product = objectMapper.readTree(created.getResponse().getContentAsString());
        String productId = product.get("id").asText();

        mockMvc.perform(get("/api/v1/products/" + productId)
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));

        mockMvc.perform(post("/api/v1/stock/movements").with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "type", "EXIT",
                                "quantity", 3,
                                "reason", "Teste automatizado",
                                "observation", "Saída de validação"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.previousQuantity").value(5))
                .andExpect(jsonPath("$.resultingQuantity").value(2));

        mockMvc.perform(get("/api/v1/products/" + productId)
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("LOW"));

        mockMvc.perform(get("/api/v1/stock/movements")
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .queryParam("productId", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("EXIT"));

        mockMvc.perform(post("/api/v1/stock/movements").with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "type", "EXIT",
                                "quantity", 10,
                                "reason", "Saída inválida"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/products/" + productId).with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + productId)
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void duplicateProductNameIsRejected() throws Exception {
        MockHttpSession session = login("gerente@gestao.local", "gestao123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> payload = Map.of(
                "name", "Produto Duplicado " + suffix,
                "sku", "DUP-" + suffix,
                "category", "Testes",
                "minimum", 1,
                "unit", "un");

        mockMvc.perform(post("/api/v1/products").with(csrf()).session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products").with(csrf()).session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    @Test
    void managerCannotAccessAnotherStore() throws Exception {
        MockHttpSession session = login("gerente@gestao.local", "gestao123");

        mockMvc.perform(get("/api/v1/products")
                        .session(session)
                        .queryParam("storeId", STORE_TWO))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/products")
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isOk());
    }

    @Test
    void operatorCannotCreateProducts() throws Exception {
        MockHttpSession session = login("operador@gestao.local", "gestao123");

        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Produto Operador " + UUID.randomUUID(),
                                "category", "Testes",
                                "minimum", 1,
                                "unit", "un"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void saleUpdatesStockAndAppearsInReports() throws Exception {
        MockHttpSession session = login("gerente@gestao.local", "gestao123");

        MvcResult products = mockMvc.perform(get("/api/v1/products")
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .queryParam("search", "Arroz 5 kg"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(products.getResponse().getContentAsString()).get("content");
        assertThat(content).isNotEmpty();
        JsonNode rice = content.get(0);
        int available = rice.get("quantity").asInt();

        mockMvc.perform(post("/api/v1/sales").with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(Map.of(
                                        "productId", rice.get("id").asText(),
                                        "quantity", 1,
                                        "unitPrice", 32.90))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(32.90));

        mockMvc.perform(get("/api/v1/products/" + rice.get("id").asText())
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(available - 1));

        mockMvc.perform(get("/api/v1/reports/sales")
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCount").isNumber());
    }

    @Test
    void createdProductCanBeUpdatedImmediately() throws Exception {
        MockHttpSession session = login("gerente@gestao.local", "gestao123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mockMvc.perform(post("/api/v1/products").with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Produto Versão " + suffix,
                                "sku", "VER-" + suffix,
                                "category", "Testes",
                                "quantity", 4,
                                "minimum", 1,
                                "unit", "un"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(4))
                .andReturn();

        JsonNode product = objectMapper.readTree(created.getResponse().getContentAsString());
        String productId = product.get("id").asText();
        long version = product.get("version").asLong();

        mockMvc.perform(put("/api/v1/products/" + productId).with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Produto Versão Editado " + suffix,
                                "sku", "VER-" + suffix,
                                "category", "Testes",
                                "minimum", 2,
                                "unit", "un",
                                "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(version + 1));

        mockMvc.perform(put("/api/v1/products/" + productId).with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Produto Versão Antiga " + suffix,
                                "sku", "VER-" + suffix,
                                "category", "Testes",
                                "minimum", 2,
                                "unit", "un",
                                "version", version))))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/products/" + productId).with(csrf())
                        .session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isNoContent());
    }

    @Test
    void archivedProductNameAndSkuCanBeReused() throws Exception {
        MockHttpSession session = login("gerente@gestao.local", "gestao123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> payload = Map.of(
                "name", "Produto Reuso " + suffix,
                "sku", "REU-" + suffix,
                "category", "Testes",
                "quantity", 2,
                "minimum", 1,
                "unit", "un");

        MvcResult created = mockMvc.perform(post("/api/v1/products").with(csrf()).session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/products/" + firstId).with(csrf()).session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isNoContent());

        MvcResult recreated = mockMvc.perform(post("/api/v1/products").with(csrf()).session(session)
                        .queryParam("storeId", STORE_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondId = objectMapper.readTree(recreated.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/products/" + secondId).with(csrf()).session(session)
                        .queryParam("storeId", STORE_ONE))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownPathsAndMethodsReturnProperErrors() throws Exception {
        MockHttpSession session = login("admin@gestao.local", "gestao123");

        mockMvc.perform(get("/api/v1/rota-inexistente").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(patch("/api/v1/products").with(csrf()).session(session))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
