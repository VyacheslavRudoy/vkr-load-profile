package ru.rudoy.loadprofile.demo.traffic;

import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Сценарии поведения пользователей на стенде.
 * <p>
 * Сценарий — цепочка обращений к сервисам от имени одного пользователя.
 * Значения, взятые из ответов (товар из списка, номер созданного заказа),
 * передаются в следующие запросы: так на стенде появляются коррелируемые
 * параметры. Между шагами выдерживается время обдумывания.
 */
@Component
class ShoppingScenarios {

    private static final List<String> CATEGORIES = List.of("books", "electronics", "home", "sport");
    private static final int CUSTOMER_COUNT = 20;
    private static final int MAX_QUANTITY = 3;

    /** Пользователь открыл общий список товаров и закрыл сайт. */
    void quickBrowse(StandClient stand, Random random) throws Exception {
        stand.productIds();
    }

    /** Полистал одну категорию и открыл страницу товара из неё. */
    void browseAndView(StandClient stand, Random random) throws Exception {
        List<Long> category = stand.productIds(anyCategory(random));
        stand.think(random);
        stand.openProduct(anyFrom(category, random));
    }

    /** Выбрал товар в категории и довёл покупку до подтверждения заказа. */
    void fullPurchase(StandClient stand, Random random) throws Exception {
        List<Long> category = stand.productIds(anyCategory(random));
        stand.think(random);
        long productId = anyFrom(category, random);
        stand.openProduct(productId);
        stand.think(random);
        String orderId = stand.createOrder(1 + random.nextInt(CUSTOMER_COUNT));
        stand.think(random);
        stand.addItem(orderId, productId, 1 + random.nextInt(MAX_QUANTITY));
        stand.think(random);
        stand.checkout(orderId);
        stand.think(random);
        stand.openOrder(orderId);
    }

    private static String anyCategory(Random random) {
        return CATEGORIES.get(random.nextInt(CATEGORIES.size()));
    }

    private static long anyFrom(List<Long> values, Random random) {
        return values.get(random.nextInt(values.size()));
    }
}
