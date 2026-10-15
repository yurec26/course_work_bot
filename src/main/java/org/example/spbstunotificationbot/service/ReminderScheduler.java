package org.example.spbstunotificationbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.repository.ReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private final ReminderService reminderService;
    private final ReminderRepository reminderRepository;

    @Scheduled(fixedDelay = 10_000)
    public void sendReminders() {

        var reminders = reminderRepository
                .findByStatusAndRemindAtLessThanEqual(Reminder.Status.PENDING, LocalDateTime.now(MOSCOW));

        for (Reminder reminder : reminders) {
            try {
                if (reminderService.sendReminder(reminder.getId())) {
                    log.info("Напоминание {} отправлено", reminder.getId());
                }
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки напоминания {}", reminder.getId(), e);
            }
        }
    }
}