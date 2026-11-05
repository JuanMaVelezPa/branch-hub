package dev.jotaeme.branchhub.application.common.pagination;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortOrderTest {

    private static final Set<String> ALLOWED = Set.of("code", "name");

    @Test
    void parsesValidSort() {
        SortOrder s = SortOrder.parse("name,desc", ALLOWED, SortOrder.asc("code"));
        assertEquals("name", s.property());
        assertEquals(SortDirection.DESC, s.direction());
    }

    @Test
    void blankUsesDefault() {
        SortOrder def = SortOrder.asc("code");
        assertEquals(def, SortOrder.parse("  ", ALLOWED, def));
    }

    @Test
    void rejectsInvalidField() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SortOrder.parse("email,asc", ALLOWED, SortOrder.asc("code")));
    }
}
