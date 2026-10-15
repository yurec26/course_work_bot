
package org.example.spbstunotificationbot.controller;

import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.service.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Поднимает только веб-часть Spring, а не всё приложение
@WebMvcTest(ReminderController.class)
@DisplayName("Тесты REST API ReminderController")
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ReminderService reminderService;

    @Test
    @DisplayName("GET: получение активных напоминаний — 200")
    void getActiveReminders_success() throws Exception {
        Reminder reminder = new Reminder();
        reminder.setId(1L);
        reminder.setChatId(123L);
        reminder.setUsername("student");
        reminder.setMessage("Сдать лабораторную");
        reminder.setRemindAt(LocalDateTime.of(2026, 10, 20, 15, 0));
        reminder.setStatus(Reminder.Status.PENDING);
        when(reminderService.getActiveReminders()).thenReturn(List.of(reminder));
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("student"))
                .andExpect(jsonPath("$[0].message")
                        .value("Сдать лабораторную"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        verify(reminderService).getActiveReminders();
    }

    @Test
    @DisplayName("GET: отсутствие активных напоминаний — пустой список")
    void getActiveReminders_empty() throws Exception {
        when(reminderService.getActiveReminders()).thenReturn(List.of());
        mockMvc.perform(get("/api/reminders")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST: успешная отправка напоминания — 204")
    void sendReminder_success() throws Exception {
        when(reminderService.sendReminder(1L)).thenReturn(true);
        mockMvc.perform(post("/api/reminders/1/send")).andExpect(status().isNoContent());
        verify(reminderService).sendReminder(1L);
    }

    @Test
    @DisplayName("POST: напоминание не найдено — 404")
    void sendReminder_notFound() throws Exception {
        when(reminderService.sendReminder(999L)).thenThrow(new NoSuchElementException("Напоминание не найдено"));
        mockMvc.perform(post("/api/reminders/999/send")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST: напоминание уже отправлено — 409")
    void sendReminder_alreadySent() throws Exception {
        when(reminderService.sendReminder(1L)).thenReturn(false);
        mockMvc.perform(post("/api/reminders/1/send")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST: ошибка Telegram API — 502")
    void sendReminder_telegramError() throws Exception {
        when(reminderService.sendReminder(1L)).thenThrow(new TelegramApiException("Telegram unavailable"));
        mockMvc.perform(post("/api/reminders/1/send")).andExpect(status().isBadGateway());
    }
}
