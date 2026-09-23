package ru.rudoy.loadprofile.demo.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.rudoy.loadprofile.demo.tracing.SessionTracingConfiguration;

/**
 * Сервис каталога товаров демонстрационного стенда.
 */
@SpringBootApplication
@Import(SessionTracingConfiguration.class)
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
