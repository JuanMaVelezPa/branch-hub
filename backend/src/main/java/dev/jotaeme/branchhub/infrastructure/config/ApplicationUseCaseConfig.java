package dev.jotaeme.branchhub.infrastructure.config;

import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.branch.usecase.CreateBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.DeactivateBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.GetBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.ListBranchesUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.UpdateBranchUseCase;
import dev.jotaeme.branchhub.application.notification.port.OutboxEventRepositoryPort;
import dev.jotaeme.branchhub.application.security.port.LoginAttemptPort;
import dev.jotaeme.branchhub.application.security.port.TokenProviderPort;
import dev.jotaeme.branchhub.application.security.port.UserCredentialsPort;
import dev.jotaeme.branchhub.application.security.port.UserDirectoryPort;
import dev.jotaeme.branchhub.application.security.port.AdminAuditLogPort;
import dev.jotaeme.branchhub.application.security.port.AdminUserPort;
import dev.jotaeme.branchhub.application.security.port.IamAuditPort;
import dev.jotaeme.branchhub.application.security.usecase.AuthenticateUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.RefreshTokenUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminCreateUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminGetUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminListAuditLogUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminListUsersUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminSetUserEnabledUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminSetUserRolesUseCase;
import dev.jotaeme.branchhub.application.demo.model.DemoAccess;
import dev.jotaeme.branchhub.application.demo.port.DemoDataPort;
import dev.jotaeme.branchhub.application.demo.port.DemoSampleDataPort;
import dev.jotaeme.branchhub.application.demo.usecase.GetDemoAccessUseCase;
import dev.jotaeme.branchhub.application.demo.usecase.ResetDemoDataUseCase;
import dev.jotaeme.branchhub.infrastructure.demo.DemoProperties;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

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
            LoginAttemptPort loginAttemptPort) {
        return new AuthenticateUserUseCase(userCredentialsPort, tokenProviderPort, userDirectoryPort, loginAttemptPort);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(TokenProviderPort tokenProviderPort, UserDirectoryPort userDirectoryPort) {
        return new RefreshTokenUseCase(tokenProviderPort, userDirectoryPort);
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
    AdminSetUserEnabledUseCase adminSetUserEnabledUseCase(AdminUserPort adminUserPort, IamAuditPort auditPort) {
        return new AdminSetUserEnabledUseCase(adminUserPort, auditPort);
    }

    @Bean
    AdminSetUserRolesUseCase adminSetUserRolesUseCase(AdminUserPort adminUserPort, IamAuditPort auditPort) {
        return new AdminSetUserRolesUseCase(adminUserPort, auditPort);
    }

    @Bean
    GetDemoAccessUseCase getDemoAccessUseCase(DemoProperties demoProperties) {
        DemoAccess access = demoProperties.enabled()
                ? new DemoAccess(
                        demoProperties.user().email(),
                        demoProperties.user().password(),
                        demoProperties.grafanaUrl())
                : null;
        return new GetDemoAccessUseCase(access);
    }

    /**
     * Only exists while the demo is on: the reset restores the demo user. Every public method runs
     * in one transaction, set here so the use case stays free of Spring types.
     */
    @Bean
    @ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
    ResetDemoDataUseCase resetDemoDataUseCase(
            DemoDataPort demoDataPort,
            DemoSampleDataPort demoSampleDataPort,
            DemoProperties demoProperties,
            TransactionManager transactionManager) {
        var target = new ResetDemoDataUseCase(
                demoDataPort,
                demoSampleDataPort,
                demoProperties.user().email(),
                demoProperties.user().password());
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new MatchAlwaysTransactionAttributeSource()));
        return (ResetDemoDataUseCase) proxy.getProxy();
    }
}
