package ru.rudoy.loadprofile.demo.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.rudoy.loadprofile.demo.tracing.SessionTracingConfiguration;

/**
 * Сервис заказов демонстрационного стенда.
 */
@SpringBootApplication
@Import(SessionTracingConfiguration.class)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
