package com.jm.spring_web.application.email.model;

import java.util.Map;
import java.util.Objects;

/**
 * Enqueue an HTML email rendered from a Thymeleaf template under {@code templates/email/}.
 *
 * @param templateId stable id (e.g. {@code GENERIC_NOTICE}) registered in {@code IamEmailTemplateId}.
 * @param toAddress recipient (single address for now).
 * @param subjectOverride optional; when null the template default subject is used.
 * @param templateVariables passed to Thymeleaf (JSON-serializable values).
 */
public record EmailEnqueueCommand(
        String templateId,
        String toAddress,
        String subjectOverride,
        Map<String, Object> templateVariables) {

    public EmailEnqueueCommand {
        Objects.requireNonNull(templateId);
        Objects.requireNonNull(toAddress);
        Objects.requireNonNull(templateVariables);
    }
}
