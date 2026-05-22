package com.jm.spring_web.infrastructure.email;

/**
 * Registered IAM email templates (English). Add entries when you add {@code templates/email/*.html}.
 */
public enum IamEmailTemplateId {
    /**
     * Generic security / account notice — variables: {@code title}, {@code lead}, {@code detail} (optional).
     */
    GENERIC_NOTICE("GENERIC_NOTICE", "email/generic-notice", "Security notice"),

    /**
     * Account re-enabled after being disabled — variables: {@code title}, {@code lead}, {@code detail} (optional).
     */
    ACCOUNT_ACTIVATED("ACCOUNT_ACTIVATED", "email/account-activated", "Your account is active");

    private final String templateId;
    private final String thymeleafPath;
    private final String defaultSubject;

    IamEmailTemplateId(String templateId, String thymeleafPath, String defaultSubject) {
        this.templateId = templateId;
        this.thymeleafPath = thymeleafPath;
        this.defaultSubject = defaultSubject;
    }

    public String templateId() {
        return templateId;
    }

    public String thymeleafPath() {
        return thymeleafPath;
    }

    public String defaultSubject() {
        return defaultSubject;
    }

    public static IamEmailTemplateId fromTemplateId(String id) {
        for (IamEmailTemplateId v : values()) {
            if (v.templateId.equals(id)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown email template id: " + id);
    }
}
