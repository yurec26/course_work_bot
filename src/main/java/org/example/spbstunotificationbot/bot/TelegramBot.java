package org.example.spbstunotificationbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.spbstunotificationbot.service.AiService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBot implements LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final AiService aiService;

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::processUpdate);
    }

    private void processUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        var message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();
        String username = message.getFrom() != null ? message.getFrom().getUserName() : null;

        log.info("Сообщение от {}: {}", chatId, text);
        String response;

        try {
            response = aiService.ask(text, chatId, username);
        } catch (Exception e) {
            log.error("Ошибка работы модели", e);
            response = "Не удалось обработать запрос. Попробуй ещё раз.";
        }

        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(response == null || response.isBlank()
                    ? "Не удалось сформировать ответ." : response).build()
            );
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в Телеграмм", e);
        }
    }
}