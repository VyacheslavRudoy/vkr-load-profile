package ru.rudoy.loadprofile.demo.order;

/**
 * Попытка добавить в заказ товар, которого нет в каталоге.
 */
class UnknownProductException extends RuntimeException {

    UnknownProductException(long productId) {
        super("Unknown product: " + productId);
    }
}
