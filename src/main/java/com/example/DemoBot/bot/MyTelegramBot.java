package com.example.DemoBot.bot;

import com.example.DemoBot.constants.Actions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

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
                case Actions.TABLE_AIRPORT -> {
                    InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                            .keyboardRow(List.of(
                                    InlineKeyboardButton.builder()
                                            .text("Add Airport")
                                            .callbackData(Actions.AIRPORT_ADD)
                                            .build(),
                                    InlineKeyboardButton.builder()
                                            .text("Delete Airport")
                                            .callbackData(Actions.AIRPORT_DELETE)
                                            .build()))
                            .keyboardRow(List.of(
                                    InlineKeyboardButton.builder()
                                            .text("Edit Airport")
                                            .callbackData(Actions.AIRPORT_EDIT)
                                            .build(),
                                    InlineKeyboardButton.builder()
                                            .text("Show all Airports")
                                            .callbackData(Actions.AIRPORT_SHOW_ALL)
                                            .build()
                            ))
                            .build();

                    try {
                    execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup.builder()
                            .chatId(callbackQuery.getMessage().getChatId().toString())
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .replyMarkup(markup)
                            .build());
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
