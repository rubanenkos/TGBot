package com.example.DemoBot.bot;

import com.example.DemoBot.model.Flight;
import com.example.DemoBot.repository.FlightRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FlightCallbackHandler {
    private final FlightRepository flightRepository;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Flight> tempFlights = new HashMap<>();

    public FlightCallbackHandler(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public boolean handleCallback(String callbackData, CallbackQuery callbackQuery, MyTelegramBot bot) {
        switch (callbackData) {
            case "flight_show_all" -> {
                handleShowAll(callbackQuery, bot);
                return true;
            }
            case "flight_add" -> {
                handleAdd(callbackQuery, bot);
                return true;
            }
            case "flight_delete" -> {
                handleDelete(callbackQuery, bot);
                return true;
            }
            case "flight_edit" -> {
                handleEdit(callbackQuery, bot);
                return true;
            }
        }
        return false;
    }

    public boolean handleFlightMessage(Update update, MyTelegramBot bot) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            String state = userStates.get(chatId);
            if ("awaiting_flight_number".equals(state)) {
                Flight flight = new Flight();
                flight.setFlightNumber(text);
                tempFlights.put(chatId, flight);
                userStates.put(chatId, "awaiting_departure_airport_id");
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Введите ID аэропорта отправления:")
                        .build();
                try {
                    bot.execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return true;
            } else if ("awaiting_departure_airport_id".equals(state)) {
                try {
                    Long airportId = Long.parseLong(text);
                    Flight flight = tempFlights.get(chatId);
                    com.example.DemoBot.model.Airport depAirport = new com.example.DemoBot.model.Airport();
                    depAirport.setId(airportId);
                    flight.setDepartureAirport(depAirport);
                    userStates.put(chatId, "awaiting_arrival_airport_id");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите ID аэропорта прибытия:")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите числовой ID аэропорта отправления:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_arrival_airport_id".equals(state)) {
                try {
                    Long airportId = Long.parseLong(text);
                    Flight flight = tempFlights.get(chatId);
                    com.example.DemoBot.model.Airport arrAirport = new com.example.DemoBot.model.Airport();
                    arrAirport.setId(airportId);
                    flight.setArrivalAirport(arrAirport);
                    userStates.put(chatId, "awaiting_departure_time");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите дату и время отправления (например, 2025-05-20 10:00):")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите числовой ID аэропорта прибытия:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_departure_time".equals(state)) {
                try {
                    java.time.LocalDateTime depTime = java.time.LocalDateTime.parse(text.replace(" ", "T"));
                    Flight flight = tempFlights.get(chatId);
                    flight.setDepartureTime(depTime);
                    userStates.put(chatId, "awaiting_arrival_time");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите дату и время прибытия (например, 2025-05-20 12:00):")
                            .build();
                    bot.execute(msg);
                } catch (Exception e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите дату и время в формате 2025-05-20 10:00:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_arrival_time".equals(state)) {
                try {
                    java.time.LocalDateTime arrTime = java.time.LocalDateTime.parse(text.replace(" ", "T"));
                    Flight flight = tempFlights.get(chatId);
                    flight.setArrivalTime(arrTime);
                    // Сохраняем рейс
                    flightRepository.save(flight);
                    userStates.remove(chatId);
                    tempFlights.remove(chatId);
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Рейс успешно добавлен!")
                            .build();
                    bot.execute(msg);
                } catch (Exception e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите дату и время в формате 2025-05-20 12:00:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            }
        }
        return false;
    }

    private void handleShowAll(CallbackQuery callbackQuery, MyTelegramBot bot) {
        List<Flight> flights = flightRepository.findAll();
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (flights.isEmpty()) {
            sb.append("Рейсы не найдены.");
        } else {
            sb.append("Список рейсов:\n");
            for (Flight f : flights) {
                String depAirport = f.getDepartureAirport() != null ? f.getDepartureAirport().getName() : "-";
                String arrAirport = f.getArrivalAirport() != null ? f.getArrivalAirport().getName() : "-";
                String depTime = f.getDepartureTime() != null ? f.getDepartureTime().format(formatter) : "-";
                String arrTime = f.getArrivalTime() != null ? f.getArrivalTime().format(formatter) : "-";
                sb.append("ID: ").append(f.getId())
                  .append(", Номер: ").append(f.getFlightNumber())
                  .append(", Маршрут: ").append(depAirport).append("->").append(arrAirport)
                  .append(", Отправление: ").append(depTime)
                  .append(", Прибытие:  ").append(arrTime)
                  .append("\n");
            }
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
        userStates.put(chatId, "awaiting_flight_number");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Введите номер рейса:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDelete(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_flight_delete_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Введите ID рейса для удаления:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEdit(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_flight_edit_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Введите ID рейса для редактирования:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Методы для работы с состояниями пользователя (get/set/remove)
    public String getUserState(Long chatId) {
        return userStates.get(chatId);
    }
    public void setUserState(Long chatId, String state) {
        userStates.put(chatId, state);
    }
    public void removeUserState(Long chatId) {
        userStates.remove(chatId);
    }
    public Flight getTempFlight(Long chatId) {
        return tempFlights.get(chatId);
    }
    public void setTempFlight(Long chatId, Flight flight) {
        tempFlights.put(chatId, flight);
    }
    public void removeTempFlight(Long chatId) {
        tempFlights.remove(chatId);
    }
    public FlightRepository getFlightRepository() {
        return flightRepository;
    }
}

