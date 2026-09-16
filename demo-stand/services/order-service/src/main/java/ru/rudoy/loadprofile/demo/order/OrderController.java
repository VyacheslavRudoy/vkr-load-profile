package ru.rudoy.loadprofile.demo.order;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Все входные данные принимаются в пути и строке запроса, тел запросов нет:
 * профиль нагрузки параметризует только их.
 */
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orders;

    OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** Создаёт пустой заказ покупателя. */
    @PostMapping
    ResponseEntity<Order> create(@RequestParam long customerId) {
        requirePositive(customerId, "customerId");
        Order order = orders.create(customerId);
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId())).body(order);
    }

    @GetMapping
    List<Order> list(@RequestParam long customerId) {
        return orders.findByCustomer(customerId);
    }

    @GetMapping("/{id}")
    Order get(@PathVariable UUID id) {
        return find(id);
    }

    @PostMapping("/{id}/items")
    Order addItem(@PathVariable UUID id, @RequestParam long productId, @RequestParam int quantity) {
        requirePositive(productId, "productId");
        requirePositive(quantity, "quantity");
        return orders.addItem(find(id), productId, quantity);
    }

    @PostMapping("/{id}/checkout")
    Order checkout(@PathVariable UUID id) {
        return orders.checkout(find(id));
    }

    @ExceptionHandler(UnknownProductException.class)
    ProblemDetail unknownProduct(UnknownProductException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Нарушение правил заказа, например добавление товара в подтверждённый заказ. */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    private Order find(UUID id) {
        return orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must be positive");
        }
    }
}
