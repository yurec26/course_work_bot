package org.example.spbstunotificationbot.service;

import org.example.spbstunotificationbot.ai.ReminderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
public class AiService {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private final ChatClient chatClient;
    private final ReminderTools reminderTools;

    public AiService(ChatClient.Builder builder, ReminderTools reminderTools) {
        this.chatClient = builder.build();
        this.reminderTools = reminderTools;
    }

    public String ask(String message, Long chatId, String username) {
        String currentTime = LocalDateTime.now(MOSCOW).toString();
        return chatClient.prompt()
                .system("""
                        Ты Telegram-бот для управления напоминаниями.
                        Отвечай на русском языке.

                        Текущая дата и время по Москве: %s.

                        Когда пользователь просит создать напоминание,
                        используй инструмент createReminder.

                        Время напоминания передавай в формате
                        yyyy-MM-dd'T'HH:mm:ss.

                        Если дата или время непонятны,
                        попроси пользователя уточнить их.

                        Никогда не утверждай, что напоминание создано,
                        пока инструмент не подтвердит сохранение.
                        """.formatted(currentTime))
                .user(message)
                .tools(reminderTools)
                .toolContext(Map.of("chatId", chatId, "username", username == null ? "" : username))
                .call()
                .content();
    }
}