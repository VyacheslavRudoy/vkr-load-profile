package ru.rudoy.loadprofile.demo.order;

import java.math.BigDecimal;

/**
 * Позиция заказа: товар с ценой на момент добавления и количество.
 */
public record OrderItem(long productId, String name, BigDecimal price, int quantity) {
}
