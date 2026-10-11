package ru.rudoy.loadprofile.model;

/** Операция профиля: HTTP-метод с шаблоном пути и статистикой встречаемости. */
public record Operation(String method, String pathTemplate, long count, double share) {

    public Operation {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (pathTemplate == null || !pathTemplate.startsWith("/")) {
            throw new IllegalArgumentException("pathTemplate must start with '/': " + pathTemplate);
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive: " + count);
        }
        if (share <= 0 || share > 1) {
            throw new IllegalArgumentException("share must be in (0, 1]: " + share);
        }
    }

    public String ref() {
        return method + " " + pathTemplate;
    }
}
