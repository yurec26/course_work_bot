
package org.example.spbstunotificationbot.repository;

import org.example.spbstunotificationbot.entity.Reminder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Создаёт тестовый контекст JPA и реальную базу
@DataJpaTest
@DisplayName("Интеграционные тесты ReminderRepository")
class ReminderRepositoryTest {

    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    @DisplayName("Получение только активных напоминаний с сортировкой по времени")
    void findActiveReminders() {
        save("Позднее", 20, Reminder.Status.PENDING);
        save("Раньше", 10, Reminder.Status.PENDING);
        save("Отправлено", 5, Reminder.Status.SENT);
        var result = reminderRepository.findByStatusOrderByRemindAtAsc(Reminder.Status.PENDING);
        assertThat(result)
                .extracting(Reminder::getMessage)
                .containsExactly("Раньше", "Позднее");
    }

    @Test
    @DisplayName("Поиск напоминаний с наступившим временем отправки")
    void findDueReminders() {
        save("Просрочено", 10, Reminder.Status.PENDING);
        save("Наступило", 12, Reminder.Status.PENDING);
        save("Будущее", 15, Reminder.Status.PENDING);
        save("Уже отправлено", 9, Reminder.Status.SENT);
        var result = reminderRepository.findByStatusAndRemindAtLessThanEqual(Reminder.Status.PENDING,
                        LocalDateTime.of(2026, 10, 20, 12, 0));
        assertThat(result)
                .extracting(Reminder::getMessage)
                .containsExactlyInAnyOrder("Просрочено", "Наступило");
    }

    @Test
    @DisplayName("Получение напоминания по ID с блокировкой")
    void findByIdForUpdate() {
        Reminder saved = save("Лабораторная", 12, Reminder.Status.PENDING);
        var result = reminderRepository.findByIdForUpdate(saved.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).isEqualTo("Лабораторная");
        assertThat(result.get().getStatus()).isEqualTo(Reminder.Status.PENDING);
    }

    private Reminder save(String message, int hour, Reminder.Status status) {
        Reminder reminder = new Reminder();
        reminder.setChatId(123L);
        reminder.setUsername("student");
        reminder.setMessage(message);
        reminder.setRemindAt(LocalDateTime.of(2026, 10, 20, hour, 0));
        reminder.setStatus(status);
        return reminderRepository.saveAndFlush(reminder);
    }
}
