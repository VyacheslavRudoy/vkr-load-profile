package ru.rudoy.loadprofile.demo.catalog;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductCatalog catalog;

    ProductController(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    /** Список товаров: все или только из указанной категории. */
    @GetMapping
    List<Product> list(@RequestParam(required = false) String category) {
        return category == null ? catalog.findAll() : catalog.findByCategory(category);
    }

    @GetMapping("/{id}")
    Product get(@PathVariable long id) {
        return catalog.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }
}
