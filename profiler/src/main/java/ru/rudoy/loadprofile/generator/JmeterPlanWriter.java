package ru.rudoy.loadprofile.generator;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Собирает план теста из Freemarker-шаблонов. Шаблоны вместо программного
 * API JMeter: тот тянет сотни мегабайт зависимостей и требует JMETER_HOME,
 * а результат шаблонов сравнивается с эталонным файлом.
 */
public final class JmeterPlanWriter {

    private final Configuration freemarker;

    public JmeterPlanWriter() {
        freemarker = new Configuration(Configuration.VERSION_2_3_32);
        // ведущий слэш: путь от корня classpath, а не от пакета этого класса
        freemarker.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates/jmeter"));
        // числа без разделителей групп и точка в дробях независимо от локали машины:
        // в XML плана нужно 1000 и 33.33, не 1 000 и 33,33
        freemarker.setNumberFormat("computer");
        freemarker.setLocale(java.util.Locale.ENGLISH);
    }

    public String write(JmeterPlan plan) {
        try {
            Template template = freemarker.getTemplate("test-plan.ftl");
            // в шаблон уходят карты, а не записи: Freemarker не видит акцессоры record
            List<Map<String, Object>> feeders = plan.feeders().stream()
                    .map(feeder -> Map.<String, Object>of(
                            "name", feeder.name(), "values", feeder.values()))
                    .toList();
            List<Map<String, Object>> sequences = plan.sequences().stream()
                    .map(sequence -> Map.<String, Object>of(
                            "name", sequence.name(),
                            "weight", sequence.weight(),
                            "steps", sequence.steps().stream()
                                    .map(request -> Map.<String, Object>of(
                                            "method", request.method(),
                                            "path", request.path(),
                                            "query", request.query().stream()
                                                    .map(param -> Map.of("name", param.name(), "value", param.value()))
                                                    .toList(),
                                            "extractions", request.extractions().stream()
                                                    .map(extraction -> Map.of(
                                                            "variable", extraction.variable(),
                                                            "jsonPath", extraction.jsonPath()))
                                                    .toList()))
                                    .toList()))
                    .toList();
            StringWriter out = new StringWriter();
            template.process(Map.of(
                    "host", plan.host(),
                    "port", plan.port(),
                    "threads", plan.load().threads(),
                    "rampSeconds", plan.load().rampSeconds(),
                    "timerDelayMs", plan.load().timer().constantDelayMs(),
                    "timerDeviationMs", plan.load().timer().deviationMs(),
                    "feeders", feeders,
                    "sequences", sequences), out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("cannot render test plan template", e);
        }
    }

    /** CSV-файлы фидеров: имя файла — содержимое, по одному значению в строке. */
    public Map<String, String> csvFiles(JmeterPlan plan) {
        Map<String, String> files = new java.util.LinkedHashMap<>();
        for (JmeterPlan.Feeder feeder : plan.feeders()) {
            files.put(feeder.name() + ".csv", String.join("\n", feeder.values()) + "\n");
        }
        return files;
    }
}
