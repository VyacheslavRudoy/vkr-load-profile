package ru.rudoy.loadprofile.demo.catalog;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "otel.sdk.disabled=true")
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductCatalog catalog;

    @Test
    void listsAllProducts() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(catalog.findAll().size()))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void filtersProductsByCategory() throws Exception {
        mvc.perform(get("/api/products").param("category", "books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].category", everyItem(is("books"))));
    }

    @Test
    void returnsProductById() throws Exception {
        mvc.perform(get("/api/products/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Headphones"))
                .andExpect(jsonPath("$.price").value(49.90));
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        mvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: 999"));
    }
}
