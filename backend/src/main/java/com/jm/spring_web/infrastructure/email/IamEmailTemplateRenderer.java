package com.jm.spring_web.infrastructure.email;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Component
public class IamEmailTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public IamEmailTemplateRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderHtml(IamEmailTemplateId template, Map<String, Object> variables) {
        Context ctx = new Context();
        if (variables != null) {
            variables.forEach(ctx::setVariable);
        }
        return templateEngine.process(template.thymeleafPath(), ctx);
    }
}
