package ru.rudoy.loadprofile.demo.order;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Клиент сервиса каталога.
 * <p>
 * Сервис заказов не хранит товары: название и цену он запрашивает у каталога
 * в момент добавления товара в заказ. Так в трассировках появляется вызов
 * между сервисами.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    CatalogClient(RestClient.Builder builder, @Value("${catalog.url}") String catalogUrl) {
        this.restClient = builder.baseUrl(catalogUrl).build();
    }

    /** Возвращает товар или пустое значение, если товара с таким id в каталоге нет. */
    public Optional<CatalogProduct> findProduct(long productId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(CatalogProduct.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
