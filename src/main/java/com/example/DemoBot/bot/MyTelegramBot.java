package com.example.DemoBot.bot;

import com.example.DemoBot.constants.Actions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
        System.out.println("Received update: " + update);
        if (update.hasCallbackQuery()){
            var callbackQuery = update.getCallbackQuery();
            switch (callbackQuery.getData()){
                case Actions.SOME_ACTION -> {
                    System.out.println("Action performed: " + Actions.SOME_ACTION);

                    try {
                        sendApiMethod(new SendMessage(
                                callbackQuery.getMessage().getChatId().toString(),
                                "It was some action performed!"));
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                sendApiMethod(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("Something happened")
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
