package dev.jotaeme.branchhub.infrastructure.notification;

import dev.jotaeme.branchhub.application.notification.model.BranchCreatedNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HrEmailService {
    private static final Logger logger = LoggerFactory.getLogger(HrEmailService.class);

    public void sendNewBranchEmail(BranchCreatedNotificationEvent event) {
        // Placeholder for SMTP provider integration.
        logger.info(
                "RRHH notification email sent for branch {} ({}) in {}",
                event.code(),
                event.name(),
                event.city()
        );
    }
}
