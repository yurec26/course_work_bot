
package org.example.spbstunotificationbot.service;

import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.repository.ReminderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Модульные тесты ReminderService")
class ReminderServiceTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private TelegramSender telegramSender;
    @InjectMocks
    private ReminderService reminderService;

    @Test
    @DisplayName("Создание напоминания с корректными данными")
    void createReminder_validData() {
        LocalDateTime date = LocalDateTime.now(MOSCOW).plusDays(1);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        Reminder result = reminderService
                .createReminder(123L, "student", "Сдать лабораторную", date);

        assertThat(result.getChatId()).isEqualTo(123L);
        assertThat(result.getUsername()).isEqualTo("student");
        assertThat(result.getMessage()).isEqualTo("Сдать лабораторную");
        assertThat(result.getRemindAt()).isEqualTo(date);
        assertThat(result.getStatus()).isEqualTo(Reminder.Status.PENDING);
        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    @DisplayName("Запрет создания напоминания с датой в прошлом")
    void createReminder_pastDate() {
        LocalDateTime date = LocalDateTime.now(MOSCOW).minusDays(1);
        assertThatThrownBy(() -> reminderService.createReminder(123L, "student", "Тест", date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Дата напоминания должна быть в будущем");
        verifyNoInteractions(reminderRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Запрет создания напоминания с пустым текстом")
    void createReminder_invalidMessage(String message) {
        LocalDateTime date = LocalDateTime.now(MOSCOW).plusDays(1);
        assertThatThrownBy(() -> reminderService.createReminder(123L, "student", message, date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Текст напоминания не может быть пустым");
        verifyNoInteractions(reminderRepository);
    }

    @Test
    @DisplayName("Запрет создания напоминания без даты")
    void createReminder_nullDate() {
        assertThatThrownBy(() -> reminderService.createReminder(
                123L, "student", "Тест", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Дата напоминания должна быть в будущем");
        verifyNoInteractions(reminderRepository);
    }

    @Test
    @DisplayName("Получение списка активных напоминаний")
    void getActiveReminders_success() {
        Reminder first = new Reminder();
        first.setId(1L);
        Reminder second = new Reminder();
        second.setId(2L);
        when(reminderRepository.findByStatusOrderByRemindAtAsc(Reminder.Status.PENDING))
                .thenReturn(List.of(first, second));
        List<Reminder> result = reminderService.getActiveReminders();
        assertThat(result).containsExactly(first, second);
        verify(reminderRepository).findByStatusOrderByRemindAtAsc(Reminder.Status.PENDING);
    }

    @Test
    @DisplayName("Успешная отправка напоминания и изменение статуса")
    void sendReminder_success() throws TelegramApiException {
        Reminder reminder = pendingReminder();
        when(reminderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reminder));
        boolean result = reminderService.sendReminder(1L);
        assertThat(result).isTrue();
        assertThat(reminder.getStatus()).isEqualTo(Reminder.Status.SENT);
        verify(telegramSender).send(123L, "⏰ Напоминание: Сдать лабораторную");
    }

    @Test
    @DisplayName("Повторная отправка уже отправленного напоминания запрещена")
    void sendReminder_alreadySent() throws TelegramApiException {
        Reminder reminder = pendingReminder();
        reminder.setStatus(Reminder.Status.SENT);
        when(reminderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reminder));
        boolean result = reminderService.sendReminder(1L);
        assertThat(result).isFalse();
        verifyNoInteractions(telegramSender);
    }

    @Test
    @DisplayName("Ошибка при отправке несуществующего напоминания")
    void sendReminder_notFound() {
        when(reminderRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> reminderService.sendReminder(999L))
                .isInstanceOf(NoSuchElementException.class).hasMessage("Напоминание не найдено");
        verifyNoInteractions(telegramSender);
    }

    @Test
    @DisplayName("При ошибке Telegram API напоминание остаётся PENDING")
    void sendReminder_telegramError() throws TelegramApiException {
        Reminder reminder = pendingReminder();
        when(reminderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reminder));
        doThrow(new TelegramApiException("Telegram unavailable"))
                .when(telegramSender)
                .send(123L, "⏰ Напоминание: Сдать лабораторную");
        assertThatThrownBy(() -> reminderService.sendReminder(1L)).isInstanceOf(TelegramApiException.class);
        assertThat(reminder.getStatus()).isEqualTo(Reminder.Status.PENDING);
    }

    // util

    private Reminder pendingReminder() {
        Reminder reminder = new Reminder();
        reminder.setId(1L);
        reminder.setChatId(123L);
        reminder.setUsername("student");
        reminder.setMessage("Сдать лабораторную");
        reminder.setRemindAt(LocalDateTime.now(MOSCOW).plusDays(1));
        reminder.setStatus(Reminder.Status.PENDING);
        return reminder;
    }
}