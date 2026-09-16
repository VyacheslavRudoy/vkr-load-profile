package ru.rudoy.loadprofile.demo.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Каталог товаров в памяти.
 * <p>
 * Набор товаров задан в коде и одинаков при каждом запуске, поэтому перед
 * любым прогоном стенд находится в одном и том же состоянии.
 */
@Component
class ProductCatalog {

    private final List<Product> products = List.of(
            product(1, "Novel", "books", "12.50"),
            product(2, "Detective story", "books", "9.90"),
            product(3, "Poetry collection", "books", "7.40"),
            product(4, "Cookbook", "books", "18.00"),
            product(5, "Travel guide", "books", "15.30"),
            product(6, "Children's book", "books", "8.60"),
            product(7, "Headphones", "electronics", "49.90"),
            product(8, "Wireless mouse", "electronics", "19.99"),
            product(9, "Keyboard", "electronics", "35.00"),
            product(10, "USB flash drive", "electronics", "8.75"),
            product(11, "Power bank", "electronics", "24.50"),
            product(12, "Webcam", "electronics", "39.00"),
            product(13, "Table lamp", "home", "27.80"),
            product(14, "Mug", "home", "4.99"),
            product(15, "Blanket", "home", "32.00"),
            product(16, "Photo frame", "home", "6.50"),
            product(17, "Wall clock", "home", "21.00"),
            product(18, "Cushion", "home", "11.20"),
            product(19, "Yoga mat", "sport", "22.00"),
            product(20, "Dumbbells", "sport", "44.90"),
            product(21, "Jump rope", "sport", "6.00"),
            product(22, "Water bottle", "sport", "9.50"),
            product(23, "Fitness tracker", "sport", "59.00"),
            product(24, "Resistance band", "sport", "7.99"));

    List<Product> findAll() {
        return products;
    }

    List<Product> findByCategory(String category) {
        return products.stream()
                .filter(product -> product.category().equals(category))
                .toList();
    }

    Optional<Product> findById(long id) {
        return products.stream()
                .filter(product -> product.id() == id)
                .findFirst();
    }

    private static Product product(long id, String name, String category, String price) {
        return new Product(id, name, category, new BigDecimal(price));
    }
}
