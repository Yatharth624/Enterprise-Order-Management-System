package com.enterprise.oms.notification.service;

import com.enterprise.oms.notification.domain.NotificationLog;
import com.enterprise.oms.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void sendNotification(String recipient, String subject, String content, String type) {
        log.info("Sending [{}] notification to [{}]: {}", type, recipient, subject);

        NotificationLog logEntry = NotificationLog.builder()
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .notificationType(type)
                .build();

        notificationLogRepository.save(logEntry);
    }

    public List<NotificationLog> getNotificationsForRecipient(String recipient) {
        return notificationLogRepository.findByRecipient(recipient);
    }
}
