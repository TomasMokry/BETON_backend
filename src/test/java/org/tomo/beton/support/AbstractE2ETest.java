package org.tomo.beton.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.tomo.beton.dtos.LoginRequest;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.entities.Category;
import org.tomo.beton.entities.Product;
import org.tomo.beton.entities.User;
import org.tomo.beton.repositories.*;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for end-to-end tests: full application context (real security filter chain, JWT filter,
 * controllers, services, JPA) on an in-memory H2 database, driven through MockMvc.
 * Tests are intentionally not @Transactional, so every request commits like in production;
 * the database is truncated before each test instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractE2ETest {

    private static final List<String> TABLES =
            List.of("order_items", "orders", "cart_items", "carts", "products", "categories", "users");

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected ProductRepository productRepository;
    @Autowired
    protected CartRepository cartRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        TABLES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY"));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    protected User createUser(String email, String rawPassword, Role role) {
        return userRepository.save(User.builder()
                .name("User " + email)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());
    }

    protected Category createCategory(String name) {
        var category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    protected Product createProduct(String name, Category category, String price, int amount) {
        var product = new Product();
        product.setName(name);
        product.setSize("M");
        product.setPrice(new BigDecimal(price));
        product.setDescription("Description of " + name);
        product.setHeight(10);
        product.setWidth(10);
        product.setWeight(100);
        product.setLength(10);
        product.setColor("Grey");
        product.setUrlImage("/images/" + name + ".jpg");
        product.setAmount(amount);
        product.setCategory(category);
        return productRepository.save(product);
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        var request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        var response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    /** Creates a user and returns a valid access token for them. */
    protected String tokenFor(String email, Role role) throws Exception {
        createUser(email, "password123", role);
        return loginAndGetToken(email, "password123");
    }

    protected static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
