package ru.rudoy.loadprofile.demo.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StandClientTest {

    private StandStub stub;
    private StandClient client;

    @BeforeEach
    void startStub() throws IOException {
        stub = new StandStub();
        client = new StandClient(HttpClient.newHttpClient(), "7c1d2a3e",
                URI.create(stub.baseUrl()), URI.create(stub.baseUrl()),
                new ObjectMapper(), new ThinkTime(0, 0));
    }

    @AfterEach
    void stopStub() {
        stub.close();
    }

    @Test
    void sendsSessionIdInBaggageHeaderOnEveryRequest() throws Exception {
        client.productIds("books");
        client.openProduct(7);
        client.createOrder(5);
        client.addItem(StandStub.ORDER_ID, 7, 2);
        client.checkout(StandStub.ORDER_ID);
        client.openOrder(StandStub.ORDER_ID);

        assertThat(stub.requests())
                .extracting(StandStub.RecordedRequest::baggage)
                .containsOnly("session.id=7c1d2a3e");
    }

    @Test
    void parsesIdsFromResponses() throws Exception {
        assertThat(client.productIds("books")).containsExactly(7L, 8L);
        assertThat(client.createOrder(5)).isEqualTo(StandStub.ORDER_ID);
    }

    @Test
    void failsWhenServiceReturnsError() throws Exception {
        stub.respondWithStatus(404);

        assertThatThrownBy(() -> client.openProduct(999))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }
}
