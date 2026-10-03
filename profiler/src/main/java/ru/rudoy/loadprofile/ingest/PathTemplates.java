package ru.rudoy.loadprofile.ingest;

import java.util.regex.Pattern;

/**
 * Приводит конкретные пути к шаблонам операций.
 * <p>
 * Числовые сегменты и UUID заменяются плейсхолдерами — так путь одного
 * запроса становится частью операции, общей для всех похожих запросов:
 * {@code /api/orders/42} → {@code /api/orders/{id}}. Слова, в которых цифры
 * лишь часть имени, например {@code v2}, не меняются. Шаблон — это отдельное
 * преобразование над путём: спаны хранят исходный {@code url.path}, потому что
 * реальные значения идентификаторов нужны позже для корреляции параметров.
 */
public final class PathTemplates {

    private static final Pattern UUID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private PathTemplates() {
    }

    /** Шаблон пути: числовые сегменты → {@code {id}}, UUID → {@code {uuid}}. */
    public static String template(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/': " + path);
        }
        String[] segments = path.split("/", -1);
        StringBuilder template = new StringBuilder(path.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                template.append('/');
            }
            template.append(placeholder(segments[i]));
        }
        return template.toString();
    }

    private static String placeholder(String segment) {
        if (isNumeric(segment)) {
            return "{id}";
        }
        if (segment.length() == 36 && UUID.matcher(segment).matches()) {
            return "{uuid}";
        }
        return segment;
    }

    private static boolean isNumeric(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        for (char c : segment.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
