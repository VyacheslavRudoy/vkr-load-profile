package ru.rudoy.loadprofile.demo.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Заказ покупателя.
 * <p>
 * Состояние меняется только методами этого класса. Методы синхронизированы,
 * потому что один заказ могут одновременно менять несколько запросов.
 */
public class Order {

    private final UUID id;
    private final long customerId;
    private final Instant createdAt;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.NEW;

    Order(UUID id, long customerId, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized OrderStatus getStatus() {
        return status;
    }

    public synchronized List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    /** Сумма заказа: цена каждой позиции, умноженная на количество. */
    public synchronized BigDecimal getTotal() {
        return items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Добавляет позицию. Возможно только пока заказ не подтверждён. */
    synchronized void addItem(OrderItem item) {
        requireNew("add items to");
        items.add(item);
    }

    /** Подтверждает заказ. Пустой заказ подтвердить нельзя. */
    synchronized void confirm() {
        requireNew("confirm");
        if (items.isEmpty()) {
            throw new IllegalStateException("Order " + id + " has no items");
        }
        status = OrderStatus.CONFIRMED;
    }

    private void requireNew(String action) {
        if (status != OrderStatus.NEW) {
            throw new IllegalStateException("Cannot " + action + " order " + id + " in status " + status);
        }
    }
}
