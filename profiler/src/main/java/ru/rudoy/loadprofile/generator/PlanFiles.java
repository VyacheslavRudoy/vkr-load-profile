package ru.rudoy.loadprofile.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Выписывает план теста и CSV-файлы фидеров в каталог. */
public final class PlanFiles {

    public List<Path> write(JmeterPlan plan, Path directory) throws IOException {
        Files.createDirectories(directory);
        JmeterPlanWriter writer = new JmeterPlanWriter();
        List<Path> written = new ArrayList<>();
        Path planFile = directory.resolve("test-plan.jmx");
        Files.writeString(planFile, writer.write(plan));
        written.add(planFile);
        for (var entry : writer.csvFiles(plan).entrySet()) {
            Path file = directory.resolve(entry.getKey());
            Files.writeString(file, entry.getValue());
            written.add(file);
        }
        return written;
    }
}
