package dev.jotaeme.branchhub.infrastructure.persistence.demo;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathDemoSampleDataAdapterTest {

    @Test
    void committedSampleHasTwelveBranchesWithTwoInactive() {
        var branches = new ClasspathDemoSampleDataAdapter().sampleBranches();

        assertThat(branches).extracting(DemoBranch::code)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, 12).mapToObj(i -> "BR-%03d".formatted(i)).toList());
        assertThat(branches).filteredOn(b -> !b.active()).hasSize(2);
        assertThat(branches).extracting(DemoBranch::city).doesNotContainNull().hasSizeGreaterThan(1);
        assertThat(branches.stream().map(DemoBranch::city).distinct().count()).isGreaterThan(3);
        assertThat(branches).allSatisfy(b -> {
            assertThat(b.code()).matches("^[A-Za-z0-9_-]{1,30}$");
            assertThat(b.name()).isNotBlank().hasSizeLessThanOrEqualTo(150);
            assertThat(b.city()).isNotBlank().hasSizeLessThanOrEqualTo(120);
        });
    }

    @Test
    void rejectsAMalformedLine() {
        var csv = new ByteArrayResource("code,name,city,active\nBR-001,Only three,true\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new ClasspathDemoSampleDataAdapter(csv).sampleBranches())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 2");
    }

    @Test
    void rejectsAnInvalidActiveFlag() {
        var csv = new ByteArrayResource("code,name,city,active\nBR-001,Name,City,yes\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new ClasspathDemoSampleDataAdapter(csv).sampleBranches())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active");
    }
}
