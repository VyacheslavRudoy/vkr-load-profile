package ru.rudoy.loadprofile.demo.order;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Заказы в памяти и правила работы с ними.
 * <p>
 * Заказы живут до перезапуска сервиса.
 */
@Service
class OrderService {

    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();
    private final CatalogClient catalog;

    OrderService(CatalogClient catalog) {
        this.catalog = catalog;
    }

    Order create(long customerId) {
        Order order = new Order(UUID.randomUUID(), customerId, Instant.now());
        orders.put(order.getId(), order);
        return order;
    }

    Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    /** Заказы покупателя, от старых к новым. */
    List<Order> findByCustomer(long customerId) {
        return orders.values().stream()
                .filter(order -> order.getCustomerId() == customerId)
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .toList();
    }

    /**
     * Добавляет товар в заказ. Название и цену берёт из каталога,
     * потому что сервис заказов товары не хранит.
     */
    Order addItem(Order order, long productId, int quantity) {
        CatalogProduct product = catalog.findProduct(productId)
                .orElseThrow(() -> new UnknownProductException(productId));
        order.addItem(new OrderItem(product.id(), product.name(), product.price(), quantity));
        return order;
    }

    Order checkout(Order order) {
        order.confirm();
        return order;
    }
}
