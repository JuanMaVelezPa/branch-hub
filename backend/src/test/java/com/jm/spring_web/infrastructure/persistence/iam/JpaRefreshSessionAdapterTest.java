package com.jm.spring_web.infrastructure.persistence.iam;

import com.jm.spring_web.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaRefreshSessionAdapterTest {

    @Mock
    private IamRefreshSessionJpaRepository repository;

    @Test
    void purgeExpiredBeforeDelegatesToRepository() {
        var adapter = new JpaRefreshSessionAdapter(repository, new JwtProperties("secret", 3600, 1209600, "REFRESH_TOKEN", 300));
        Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
        when(repository.deleteByExpiresAtBefore(cutoff)).thenReturn(3L);

        assertThat(adapter.purgeExpiredBefore(cutoff)).isEqualTo(3L);
        verify(repository).deleteByExpiresAtBefore(cutoff);
    }
}
