package com.jm.spring_web.infrastructure.persistence.iam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "iam_user")
public class IamUserJpaEntity {
    @Id
    private UUID id;

    @Column(unique = true, length = 320)
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret_encrypted", length = 4000)
    private String mfaSecretEncrypted;

    @Column(name = "mfa_pending_secret_encrypted", length = 4000)
    private String mfaPendingSecretEncrypted;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "iam_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<IamRoleJpaEntity> roles = new HashSet<>();

    protected IamUserJpaEntity() {
    }

    public IamUserJpaEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Set<IamRoleJpaEntity> getRoles() {
        return roles;
    }

    public void addRole(IamRoleJpaEntity role) {
        this.roles.add(role);
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecretEncrypted() {
        return mfaSecretEncrypted;
    }

    public void setMfaSecretEncrypted(String mfaSecretEncrypted) {
        this.mfaSecretEncrypted = mfaSecretEncrypted;
    }

    public String getMfaPendingSecretEncrypted() {
        return mfaPendingSecretEncrypted;
    }

    public void setMfaPendingSecretEncrypted(String mfaPendingSecretEncrypted) {
        this.mfaPendingSecretEncrypted = mfaPendingSecretEncrypted;
    }
}

