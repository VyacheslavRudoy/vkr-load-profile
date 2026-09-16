package ru.rudoy.loadprofile.demo.catalog;

import java.math.BigDecimal;

/**
 * Товар каталога.
 */
public record Product(long id, String name, String category, BigDecimal price) {
}
