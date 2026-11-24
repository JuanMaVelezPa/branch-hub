package dev.jotaeme.branchhub.infrastructure.persistence.demo;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;
import dev.jotaeme.branchhub.application.demo.port.DemoSampleDataPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the sample branches from {@code demo/demo-branches.csv}: a {@code code,name,city,active}
 * header, then one branch per line. Values must not contain commas.
 */
@Component
public class ClasspathDemoSampleDataAdapter implements DemoSampleDataPort {
    static final String RESOURCE = "demo/demo-branches.csv";

    private final Resource resource;

    public ClasspathDemoSampleDataAdapter() {
        this(new ClassPathResource(RESOURCE));
    }

    ClasspathDemoSampleDataAdapter(Resource resource) {
        this.resource = resource;
    }

    @Override
    public List<DemoBranch> sampleBranches() {
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<DemoBranch> branches = new ArrayList<>();
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }
                branches.add(parse(line, lineNumber));
            }
            return List.copyOf(branches);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    private static DemoBranch parse(String line, int lineNumber) {
        String[] cells = line.split(",", -1);
        if (cells.length != 4) {
            throw new IllegalStateException(RESOURCE + " line " + lineNumber + ": expected 4 columns, got " + cells.length);
        }
        String active = cells[3].trim();
        if (!active.equals("true") && !active.equals("false")) {
            throw new IllegalStateException(RESOURCE + " line " + lineNumber + ": active must be true or false");
        }
        return new DemoBranch(cells[0].trim(), cells[1].trim(), cells[2].trim(), Boolean.parseBoolean(active));
    }
}
