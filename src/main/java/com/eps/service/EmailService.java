package com.eps.service;

import java.util.Map;

public interface EmailService {
    void sendSimpleEmail(String to, String subject, String body);
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendHtmlEmailFromTemplate(String to, String subject, String templateName, Map<String, String> params);
}
