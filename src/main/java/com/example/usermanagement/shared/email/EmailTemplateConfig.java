package com.example.usermanagement.shared.email;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Configures an additional Thymeleaf template resolver for plain text emails.
 * <p>
 * Spring Boot auto-configures an HTML resolver (suffix .html, order 1).
 * This adds a TEXT mode resolver for .txt templates used as email
 * plain-text alternatives. The resolver uses no suffix so template
 * names include the .txt extension explicitly.
 */
@Configuration
class EmailTemplateConfig {

    /**
     * Registers a TEXT-mode resolver for .txt email templates.
     * <p>
     * Uses {@code checkExistence=true} so it only resolves templates that
     * actually exist as .txt files, falling through to the default HTML
     * resolver for .html templates.
     */
    @Bean
    ClassLoaderTemplateResolver textTemplateResolver(SpringTemplateEngine templateEngine) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix("");
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(2);
        resolver.setCheckExistence(true);
        resolver.setResolvablePatterns(java.util.Set.of("*.txt"));

        templateEngine.addTemplateResolver(resolver);
        return resolver;
    }
}
