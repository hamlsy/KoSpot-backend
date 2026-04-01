package com.kospot.auth.application.service;

import com.kospot.common.config.mail.PasswordResetProperties;
import com.kospot.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailComposer {

    private static final String TEMPLATE_NAME = "email/password-reset";
    private static final String SUBJECT = "[KoSpot] 비밀번호 재설정 안내";

    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final PasswordResetProperties properties;

    public void send(String toEmail, String resetToken) {
        String resetLink = properties.getBaseUrl() + "?token=" + resetToken;

        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        String html = templateEngine.process(TEMPLATE_NAME, context);

        emailService.sendHtmlEmail(toEmail, SUBJECT, properties.getFromEmail(), html);
    }
}
