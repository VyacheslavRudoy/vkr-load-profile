package ru.rudoy.loadprofile.demo.order;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Сервис каталога заменён заглушкой: проверяется только сервис заказов.
 */
@SpringBootTest(properties = "otel.sdk.disabled=true")
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogClient catalog;

    @Test
    void createsEmptyOrderForCustomer() throws Exception {
        mvc.perform(post("/api/orders").param("customerId", "42"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/orders/")))
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void addsItemAndConfirmsOrder() throws Exception {
        when(catalog.findProduct(7))
                .thenReturn(Optional.of(new CatalogProduct(7, "Headphones", new BigDecimal("49.90"))));
        String orderId = createOrder(7);

        mvc.perform(post("/api/orders/{id}/items", orderId).param("productId", "7").param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Headphones"))
                .andExpect(jsonPath("$.total").value(99.80));

        mvc.perform(post("/api/orders/{id}/checkout", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(get("/api/orders").param("customerId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void rejectsUnknownProduct() throws Exception {
        when(catalog.findProduct(999)).thenReturn(Optional.empty());
        String orderId = createOrder(1);

        mvc.perform(post("/api/orders/{id}/items", orderId).param("productId", "999").param("quantity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown product: 999"));
    }

    @Test
    void rejectsNonPositiveQuantity() throws Exception {
        String orderId = createOrder(2);

        mvc.perform(post("/api/orders/{id}/items", orderId).param("productId", "7").param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("quantity must be positive"));
    }

    @Test
    void rejectsCheckoutOfEmptyOrder() throws Exception {
        String orderId = createOrder(3);

        mvc.perform(post("/api/orders/{id}/checkout", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Order " + orderId + " has no items"));
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private String createOrder(long customerId) throws Exception {
        String body = mvc.perform(post("/api/orders").param("customerId", String.valueOf(customerId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
