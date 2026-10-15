package org.example.spbstunotificationbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.service.ReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public List<Reminder> getActiveReminders() {
        return reminderService.getActiveReminders();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendReminder(@PathVariable Long id) {
        try {
            if (!reminderService.sendReminder(id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Напоминание уже отправлено");
            }
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (TelegramApiException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Ошибка Telegram API", e
            );
        }
    }
}