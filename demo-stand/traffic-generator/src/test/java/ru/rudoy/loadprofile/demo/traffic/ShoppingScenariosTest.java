package ru.rudoy.loadprofile.demo.traffic;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Проверяет эталонные последовательности: какие именно запросы выдаёт
 * каждый сценарий. Это же описание попадает в методику эксперимента.
 */
class ShoppingScenariosTest {

    private static final String CATEGORY = "/api/products\\?category=(books|electronics|home|sport)";
    private static final String ORDER = StandStub.ORDER_ID;

    private final ShoppingScenarios scenarios = new ShoppingScenarios();

    private StandStub stub;
    private StandClient stand;

    @BeforeEach
    void startStub() throws IOException {
        stub = new StandStub();
        stand = new StandClient(HttpClient.newHttpClient(), "test-session",
                URI.create(stub.baseUrl()), URI.create(stub.baseUrl()),
                new ObjectMapper(), new ThinkTime(0, 0));
    }

    @AfterEach
    void stopStub() {
        stub.close();
    }

    @Test
    void quickBrowseOpensCatalogOnce() throws Exception {
        scenarios.quickBrowse(stand, new Random(1));

        assertThat(calls()).containsExactly("GET /api/products");
    }

    @Test
    void browseAndViewOpensCategoryThenProduct() throws Exception {
        scenarios.browseAndView(stand, new Random(1));

        assertThat(calls()).hasSize(2);
        assertThat(calls().get(0)).matches("GET " + CATEGORY);
        assertThat(calls().get(1)).matches("GET /api/products/(7|8)");
    }

    @Test
    void fullPurchaseFollowsTheReferenceSequence() throws Exception {
        scenarios.fullPurchase(stand, new Random(1));

        List<String> calls = calls();
        assertThat(calls).hasSize(6);
        assertThat(calls.get(0)).matches("GET " + CATEGORY);
        assertThat(calls.get(1)).matches("GET /api/products/(7|8)");
        assertThat(calls.get(2)).matches("POST /api/orders\\?customerId=\\d+");
        assertThat(calls.get(3)).matches(
                "POST /api/orders/" + ORDER + "/items\\?productId=(7|8)&quantity=[1-3]");
        assertThat(calls.get(4)).isEqualTo("POST /api/orders/" + ORDER + "/checkout");
        assertThat(calls.get(5)).isEqualTo("GET /api/orders/" + ORDER);
    }

    private List<String> calls() {
        return stub.requests().stream()
                .map(request -> request.method() + " " + request.path())
                .toList();
    }
}
