package ru.rudoy.loadprofile.demo.order;

import java.math.BigDecimal;

/**
 * Товар в том виде, в каком его отдаёт сервис каталога.
 * Поля ответа, которых здесь нет, игнорируются.
 */
public record CatalogProduct(long id, String name, BigDecimal price) {
}
