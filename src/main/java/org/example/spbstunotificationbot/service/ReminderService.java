package org.example.spbstunotificationbot.service;

import lombok.RequiredArgsConstructor;
import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.repository.ReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final TelegramSender telegramSender;
    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Transactional
    public Reminder createReminder(Long chatId, String username, String message, LocalDateTime remindAt) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Текст напоминания не может быть пустым"
            );
        }

        if (remindAt == null || !remindAt.isAfter(LocalDateTime.now(MOSCOW))) {
            throw new IllegalArgumentException(
                    "Дата напоминания должна быть в будущем"
            );
        }
        Reminder reminder = new Reminder();
        reminder.setChatId(chatId);
        reminder.setUsername(username);
        reminder.setMessage(message);
        reminder.setRemindAt(remindAt);
        reminder.setStatus(Reminder.Status.PENDING);
        return reminderRepository.save(reminder);
    }

    public List<Reminder> getActiveReminders() {
        return reminderRepository.findByStatusOrderByRemindAtAsc(Reminder.Status.PENDING);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean sendReminder(Long id) throws TelegramApiException {
        Reminder reminder = reminderRepository
                .findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Напоминание не найдено"));

        if (reminder.getStatus() != Reminder.Status.PENDING) {
            return false;
        }

        telegramSender.send(reminder.getChatId(), "⏰ Напоминание: " + reminder.getMessage());
        reminder.setStatus(Reminder.Status.SENT);
        return true;
    }
}