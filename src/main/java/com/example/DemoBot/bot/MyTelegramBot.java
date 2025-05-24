package com.example.DemoBot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.objects.Update;
@Component
public class MyTelegramBot extends TelegramLongPollingCommandBot {
    private final String username;

    public MyTelegramBot(@Value("${bot.token}") String botToken,
                         @Value("${bot.username}") String username) {
        super(botToken);
        this.username = username;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void processNonCommandUpdate(Update update) {

    }


}
