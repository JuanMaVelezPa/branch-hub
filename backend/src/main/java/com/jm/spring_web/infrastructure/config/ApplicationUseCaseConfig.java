package com.jm.spring_web.infrastructure.config;

import com.jm.spring_web.application.branch.port.BranchRepositoryPort;
import com.jm.spring_web.application.branch.usecase.CreateBranchUseCase;
import com.jm.spring_web.application.branch.usecase.DeactivateBranchUseCase;
import com.jm.spring_web.application.branch.usecase.GetBranchUseCase;
import com.jm.spring_web.application.branch.usecase.ListBranchesUseCase;
import com.jm.spring_web.application.branch.usecase.UpdateBranchUseCase;
import com.jm.spring_web.application.notification.port.OutboxEventRepositoryPort;
import com.jm.spring_web.application.email.port.EmailOutboxPort;
import com.jm.spring_web.application.security.port.AdminMfaPort;
import com.jm.spring_web.application.security.port.LoginAttemptPort;
import com.jm.spring_web.application.security.port.TotpVerificationPort;
import com.jm.spring_web.application.security.port.TokenProviderPort;
import com.jm.spring_web.application.security.port.UserCredentialsPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;
import com.jm.spring_web.application.security.port.AdminAuditLogPort;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.port.AdminUserPort;
import com.jm.spring_web.application.security.port.IamAuditPort;
import com.jm.spring_web.application.security.totp.Rfc6238Totp;
import com.jm.spring_web.application.security.usecase.AuthenticateUserUseCase;
import com.jm.spring_web.application.security.usecase.BeginAdminMfaEnrollmentUseCase;
import com.jm.spring_web.application.security.usecase.ConfirmAdminMfaEnrollmentUseCase;
import com.jm.spring_web.application.security.usecase.VerifyMfaChallengeUseCase;
import com.jm.spring_web.application.security.usecase.ListRefreshSessionsUseCase;
import com.jm.spring_web.application.security.usecase.RefreshTokenUseCase;
import com.jm.spring_web.application.security.usecase.RevokeOtherRefreshSessionsUseCase;
import com.jm.spring_web.application.security.usecase.RevokeRefreshSessionUseCase;
import com.jm.spring_web.application.security.usecase.AdminCreateUserUseCase;
import com.jm.spring_web.application.security.usecase.AdminGetUserUseCase;
import com.jm.spring_web.application.security.usecase.AdminListAuditLogUseCase;
import com.jm.spring_web.application.security.usecase.AdminListUsersUseCase;
import com.jm.spring_web.application.security.usecase.AdminSetUserEnabledUseCase;
import com.jm.spring_web.application.security.usecase.AdminSetUserRolesUseCase;
import com.jm.spring_web.infrastructure.security.MfaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationUseCaseConfig {
    @Bean
    CreateBranchUseCase createBranchUseCase(
            BranchRepositoryPort branchRepositoryPort,
            OutboxEventRepositoryPort outboxEventRepositoryPort
    ) {
        return new CreateBranchUseCase(branchRepositoryPort, outboxEventRepositoryPort);
    }

    @Bean
    GetBranchUseCase getBranchUseCase(BranchRepositoryPort branchRepositoryPort) {
        return new GetBranchUseCase(branchRepositoryPort);
    }

    @Bean
    ListBranchesUseCase listBranchesUseCase(BranchRepositoryPort branchRepositoryPort) {
        return new ListBranchesUseCase(branchRepositoryPort);
    }

    @Bean
    UpdateBranchUseCase updateBranchUseCase(BranchRepositoryPort branchRepositoryPort) {
        return new UpdateBranchUseCase(branchRepositoryPort);
    }

    @Bean
    DeactivateBranchUseCase deactivateBranchUseCase(BranchRepositoryPort branchRepositoryPort) {
        return new DeactivateBranchUseCase(branchRepositoryPort);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserCredentialsPort userCredentialsPort,
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            LoginAttemptPort loginAttemptPort,
            RefreshSessionPort refreshSessionPort) {
        return new AuthenticateUserUseCase(
                userCredentialsPort, tokenProviderPort, userDirectoryPort, loginAttemptPort, refreshSessionPort);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            RefreshSessionPort refreshSessionPort) {
        return new RefreshTokenUseCase(tokenProviderPort, userDirectoryPort, refreshSessionPort);
    }

    @Bean
    ListRefreshSessionsUseCase listRefreshSessionsUseCase(RefreshSessionPort refreshSessionPort) {
        return new ListRefreshSessionsUseCase(refreshSessionPort);
    }

    @Bean
    RevokeRefreshSessionUseCase revokeRefreshSessionUseCase(RefreshSessionPort refreshSessionPort) {
        return new RevokeRefreshSessionUseCase(refreshSessionPort);
    }

    @Bean
    RevokeOtherRefreshSessionsUseCase revokeOtherRefreshSessionsUseCase(RefreshSessionPort refreshSessionPort) {
        return new RevokeOtherRefreshSessionsUseCase(refreshSessionPort);
    }

    @Bean
    AdminCreateUserUseCase adminCreateUserUseCase(AdminUserPort adminUserPort, IamAuditPort auditPort) {
        return new AdminCreateUserUseCase(adminUserPort, auditPort);
    }

    @Bean
    AdminGetUserUseCase adminGetUserUseCase(AdminUserPort adminUserPort) {
        return new AdminGetUserUseCase(adminUserPort);
    }

    @Bean
    AdminListUsersUseCase adminListUsersUseCase(AdminUserPort adminUserPort) {
        return new AdminListUsersUseCase(adminUserPort);
    }

    @Bean
    AdminListAuditLogUseCase adminListAuditLogUseCase(AdminAuditLogPort adminAuditLogPort) {
        return new AdminListAuditLogUseCase(adminAuditLogPort);
    }

    @Bean
    AdminSetUserEnabledUseCase adminSetUserEnabledUseCase(
            AdminUserPort adminUserPort,
            IamAuditPort auditPort,
            EmailOutboxPort emailOutboxPort) {
        return new AdminSetUserEnabledUseCase(adminUserPort, auditPort, emailOutboxPort);
    }

    @Bean
    AdminSetUserRolesUseCase adminSetUserRolesUseCase(AdminUserPort adminUserPort, IamAuditPort auditPort) {
        return new AdminSetUserRolesUseCase(adminUserPort, auditPort);
    }

    @Bean
    TotpVerificationPort totpVerificationPort() {
        return (secret, code) -> Rfc6238Totp.isValid(secret, code, 1);
    }

    @Bean
    BeginAdminMfaEnrollmentUseCase beginAdminMfaEnrollmentUseCase(
            UserDirectoryPort userDirectoryPort,
            AdminMfaPort adminMfaPort,
            MfaProperties mfaProperties) {
        return new BeginAdminMfaEnrollmentUseCase(userDirectoryPort, adminMfaPort, mfaProperties.issuer());
    }

    @Bean
    ConfirmAdminMfaEnrollmentUseCase confirmAdminMfaEnrollmentUseCase(
            UserDirectoryPort userDirectoryPort,
            AdminMfaPort adminMfaPort,
            TotpVerificationPort totpVerificationPort) {
        return new ConfirmAdminMfaEnrollmentUseCase(userDirectoryPort, adminMfaPort, totpVerificationPort);
    }

    @Bean
    VerifyMfaChallengeUseCase verifyMfaChallengeUseCase(
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            AdminMfaPort adminMfaPort,
            TotpVerificationPort totpVerificationPort,
            RefreshSessionPort refreshSessionPort) {
        return new VerifyMfaChallengeUseCase(
                tokenProviderPort, userDirectoryPort, adminMfaPort, totpVerificationPort, refreshSessionPort);
    }
}
