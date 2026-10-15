package org.example.spbstunotificationbot.ai;

import lombok.RequiredArgsConstructor;
import org.example.spbstunotificationbot.entity.Reminder;
import org.example.spbstunotificationbot.service.ReminderService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReminderTools {

    private final ReminderService reminderService;

    @Tool(
            name = "createReminder",
            description = "Создать напоминание на указанную дату и время по Москве"
    )
    public String createReminder(
            @ToolParam(description = "Текст напоминания")
            String message,
            @ToolParam(description = "Дата и время в формате yyyy-MM-dd'T'HH:mm:ss")
            String remindAt,
            ToolContext context
    ) {
        Long chatId = (Long) context.getContext().get("chatId");
        String username = (String) context.getContext().get("username");

        if (chatId == null) {
            return "Ошибка: не указан Telegram chatId";
        }

        try {
            Reminder reminder = reminderService.createReminder(chatId, username, message, LocalDateTime.parse(remindAt));
            return "Напоминание создано. ID: " + reminder.getId();

        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            return "Не удалось создать напоминание: " + e.getMessage();
        }
    }
}