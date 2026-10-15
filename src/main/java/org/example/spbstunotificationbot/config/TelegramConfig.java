package org.example.spbstunotificationbot.config;

import org.example.spbstunotificationbot.bot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(@Value("${telegram.bot.token}") String token) {
        return new OkHttpTelegramClient(token);
    }

    @Bean(destroyMethod = "close")
    public TelegramBotsLongPollingApplication telegramApplication(@Value("${telegram.bot.token}") String token,
                                                                  TelegramBot bot) throws TelegramApiException {
        var application = new TelegramBotsLongPollingApplication();
        application.registerBot(token, bot);
        return application;
    }
}