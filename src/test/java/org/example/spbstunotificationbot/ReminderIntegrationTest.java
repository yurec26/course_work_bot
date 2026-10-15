
package org.example.spbstunotificationbot;

import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.repository.ReminderRepository;
import org.example.spbstunotificationbot.service.TelegramSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "telegram.bot.token=test-token",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@DisplayName("Интеграционные тесты сервиса напоминаний")
class ReminderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ReminderRepository reminderRepository;
    @MockitoBean
    private TelegramSender telegramSender;
    // Не запускаем настоящий Telegram Long Polling
    @MockitoBean
    private TelegramBotsLongPollingApplication telegramApplication;
    @BeforeEach
    void setUp() {
        reminderRepository.deleteAll();
    }

    @Test
    @DisplayName("Полный сценарий: получение и отправка напоминания через REST API")
    void reminderFullFlow() throws Exception {
        // Создаём напоминание в настоящей H2
        Reminder reminder = new Reminder();
        reminder.setChatId(123L);
        reminder.setUsername("student");
        reminder.setMessage("Сдать лабораторную");
        reminder.setRemindAt(LocalDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(1));
        reminder.setStatus(Reminder.Status.PENDING);
        Reminder saved = reminderRepository.saveAndFlush(reminder);
        Long id = saved.getId();

        // GET должен вернуть сохранённое напоминание
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Принудительно отправляем через POST
        mockMvc.perform(post("/api/reminders/{id}/send", id)).andExpect(status().isNoContent());

        //  Проверяем вызов TelegramSender
        verify(telegramSender).send(123L, "⏰ Напоминание: Сдать лабораторную");

        // Проверяем сохранение статуса SENT в H2
        Reminder updated = reminderRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Reminder.Status.SENT);

        // Отправленное напоминание исчезло из активных
        mockMvc.perform(get("/api/reminders")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
