package com.example.DemoBot.bot;

import com.example.DemoBot.constants.Actions;
import com.example.DemoBot.model.Airport;
import com.example.DemoBot.repository.AirportRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

@Component
public class AirportCallbackHandler {
    private final AirportRepository airportRepository;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Airport> tempAirports = new HashMap<>();

    public AirportCallbackHandler(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    public boolean handleCallback(String callbackData, CallbackQuery callbackQuery, MyTelegramBot bot) {
        switch (callbackData) {
            case Actions.AIRPORT_SHOW_ALL -> {
                handleShowAll(callbackQuery, bot);
                return true;
            }
            case Actions.AIRPORT_ADD -> {
                handleAdd(callbackQuery, bot);
                return true;
            }
            case Actions.AIRPORT_DELETE -> {
                handleDelete(callbackQuery, bot);
                return true;
            }
            case Actions.AIRPORT_EDIT -> {
                handleEdit(callbackQuery, bot);
                return true;
            }
        }
        return false;
    }

    public boolean handleAirportMessage(Update update, MyTelegramBot bot) {
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
                        .text("Enter airport code:")
                        .build();
                try {
                    bot.execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return true;
            } else if ("awaiting_airport_code".equals(state)) {
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
                                .text("Airport successfully added!")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Please enter a positive number for the airport code:")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (NumberFormatException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter a numeric value for the airport code:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_airport_delete_id".equals(state)) {
                try {
                    Long id = Long.parseLong(text);
                    if (airportRepository.existsById(id)) {
                        airportRepository.deleteById(id);
                        userStates.remove(chatId);
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Airport with ID " + id + " successfully deleted!")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Airport with such ID not found. Please enter correct ID:")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (NumberFormatException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter the numeric airport ID:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_airport_edit_id".equals(state)) {
                try {
                    Long id = Long.parseLong(text);
                    Airport airport = airportRepository.findById(id).orElse(null);
                    if (airport != null) {
                        tempAirports.put(chatId, airport);
                        userStates.put(chatId, "awaiting_airport_edit_name");
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Enter a new airport name (current: " + airport.getName() + "):")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Airport with such ID not found. Please enter correct ID:")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (NumberFormatException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter the numeric airport ID:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_airport_edit_name".equals(state)) {
                Airport airport = tempAirports.get(chatId);
                airport.setName(text);
                userStates.put(chatId, "awaiting_airport_edit_code");
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Enter new airport code (current: " + airport.getCode() + "):")
                        .build();
                try {
                    bot.execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return true;
            } else if ("awaiting_airport_edit_code".equals(state)) {
                Airport airport = tempAirports.get(chatId);
                try {
                    int codeInt = Integer.parseInt(text);
                    if (codeInt > 0) {
                        airport.setCode(text);
                        airportRepository.save(airport);
                        userStates.remove(chatId);
                        tempAirports.remove(chatId);
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("The airport has been successfully updated!")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Please enter a positive number for the airport code:")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (NumberFormatException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter a numeric value for the airport code:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void handleShowAll(CallbackQuery callbackQuery, MyTelegramBot bot) {
        var airports = airportRepository.findAll();
        StringBuilder sb = new StringBuilder();
        if (airports.isEmpty()) {
            sb.append("No airports found.");
        } else {
            sb.append("List of airports:\n");
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
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleAdd(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_airport_name");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Enter the airport name:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDelete(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_airport_delete_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Enter the airport ID to delete:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEdit(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_airport_edit_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Enter airport ID to edit:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserState(Long chatId) {
        return userStates.get(chatId);
    }
    public void setUserState(Long chatId, String state) {
        userStates.put(chatId, state);
    }
    public void removeUserState(Long chatId) {
        userStates.remove(chatId);
    }
    public Airport getTempAirport(Long chatId) {
        return tempAirports.get(chatId);
    }
    public void setTempAirport(Long chatId, Airport airport) {
        tempAirports.put(chatId, airport);
    }
    public void removeTempAirport(Long chatId) {
        tempAirports.remove(chatId);
    }
    public AirportRepository getAirportRepository() {
        return airportRepository;
    }
}
