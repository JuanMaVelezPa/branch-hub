package dev.jotaeme.branchhub.domain.branch;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Branch {
    private final UUID id;
    private final String code;
    private final String name;
    private final String city;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Branch(UUID id, String code, String name, String city, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.code = normalize(code);
        this.name = normalize(name);
        this.city = normalize(city);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Branch createNew(String code, String name, String city) {
        LocalDateTime now = LocalDateTime.now();
        return new Branch(UUID.randomUUID(), code, name, city, true, now, now);
    }

    public static Branch restore(
            UUID id,
            String code,
            String name,
            String city,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new Branch(id, code, name, city, active, createdAt, updatedAt);
    }

    public Branch update(String name, String city) {
        return new Branch(this.id, this.code, name, city, this.active, this.createdAt, LocalDateTime.now());
    }

    public Branch deactivate() {
        return new Branch(this.id, this.code, this.name, this.city, false, this.createdAt, LocalDateTime.now());
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public String city() {
        return city;
    }

    public boolean active() {
        return active;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value).trim();
    }
}
