package com.example.DemoBot.bot;

import com.example.DemoBot.constants.Actions;
import com.example.DemoBot.repository.AirportRepository;
import com.example.DemoBot.model.Airport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MyTelegramBot extends TelegramLongPollingCommandBot {
    private final String username;
    private final AirportRepository airportRepository;

    // Для хранения этапа добавления аэропорта по chatId
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Airport> tempAirports = new HashMap<>();

    @Autowired
    public MyTelegramBot(@Value("${bot.token}") String botToken,
                         @Value("${bot.username}") String username,
                         AirportRepository airportRepository) {
        super(botToken);
        this.username = username;
        this.airportRepository = airportRepository;
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
                case Actions.AIRPORT_SHOW_ALL -> {
                    var airports = airportRepository.findAll();
                    StringBuilder sb = new StringBuilder();
                    if (airports.isEmpty()) {
                        sb.append("Аэропорты не найдены.");
                    } else {
                        sb.append("Список аэропортов:\n");
                        airports.forEach(a -> sb.append("ID: ").append(a.getId())
                                .append(", Name: ").append(a.getName())
                                .append(", Code: ").append(a.getCode())
                                .append("\n"));
                    }
                    SendMessage msg = SendMessage.builder()
                            .chatId(callbackQuery.getMessage().getChatId().toString())
                            .text(sb.toString())
                            .build();
                    try {
                        execute(msg);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
                case Actions.AIRPORT_ADD -> {
                    Long chatId = callbackQuery.getMessage().getChatId();
                    userStates.put(chatId, "awaiting_airport_name");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите название аэропорта:")
                            .build();
                    try {
                        execute(msg);
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

        // Обработка этапов добавления аэропорта
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            String state = userStates.get(chatId);
            if ("awaiting_airport_name".equals(state)) {
                Airport airport = new Airport();
                airport.setName(text);
                tempAirports.put(chatId, airport);
                userStates.put(chatId, "awaiting_airport_code");
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Введите код аэропорта:")
                        .build();
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if ("awaiting_airport_code".equals(state)) {
                // Проверка, что введённое значение - положительное число
                try {
                    int codeInt = Integer.parseInt(text);
                    if (codeInt > 0) {
                        Airport airport = tempAirports.get(chatId);
                        airport.setCode(text);
                        airportRepository.save(airport);
                        userStates.remove(chatId);
                        tempAirports.remove(chatId);
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Аэропорт успешно добавлен!")
                                .build();
                        try {
                            execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Пожалуйста, введите положительное число для кода аэропорта:")
                                .build();
                        try {
                            execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (NumberFormatException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите числовое значение для кода аэропорта:")
                            .build();
                    try {
                        execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return;
            }
        }
    }
}
