package com.example.DemoBot.bot;

import com.example.DemoBot.model.Flight;
import com.example.DemoBot.repository.FlightRepository;
import com.example.DemoBot.repository.AirportRepository;
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
    private final AirportRepository airportRepository;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Flight> tempFlights = new HashMap<>();

    public FlightCallbackHandler(FlightRepository flightRepository, AirportRepository airportRepository) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
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
                    var depAirportOpt = airportRepository.findById(airportId);
                    if (depAirportOpt.isEmpty()) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Аэропорт с таким ID не найден. Введите корректный ID аэропорта отправления:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }
                    Flight flight = tempFlights.get(chatId);
                    flight.setDepartureAirport(depAirportOpt.get());
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
                    var arrAirportOpt = airportRepository.findById(airportId);
                    if (arrAirportOpt.isEmpty()) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Аэропорт с таким ID не найден. Введите корректный ID аэропорта прибытия:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }
                    Flight flight = tempFlights.get(chatId);
                    flight.setArrivalAirport(arrAirportOpt.get());
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
            } else if ("awaiting_flight_delete_id".equals(state)) {
                try {
                    Long id = Long.parseLong(text);
                    if (flightRepository.existsById(id)) {
                        flightRepository.deleteById(id);
                        userStates.remove(chatId);
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Рейс с ID " + id + " успешно удалён!")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Рейс с таким ID не найден. Введите корректный ID:")
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
                            .text("Пожалуйста, введите числовой ID рейса:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_flight_edit_id".equals(state)) {
                try {
                    Long id = Long.parseLong(text);
                    Flight flight = flightRepository.findById(id).orElse(null);
                    if (flight != null) {
                        tempFlights.put(chatId, flight);
                        userStates.put(chatId, "awaiting_flight_edit_number");
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Введите новый номер рейса (текущий: " + flight.getFlightNumber() + "):")
                                .build();
                        bot.execute(msg);
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Рейс с ID " + id + " не найден. Введите корректный ID:")
                                .build();
                        bot.execute(msg);
                    }
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите корректный числовой ID рейса:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_flight_edit_number".equals(state)) {
                Flight flight = tempFlights.get(chatId);
                flight.setFlightNumber(text);
                userStates.put(chatId, "awaiting_flight_edit_departure_id");
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Введите новый ID аэропорта отправления (текущий: " +
                              (flight.getDepartureAirport() != null ? flight.getDepartureAirport().getId() : "-") + "):")
                        .build();
                try { bot.execute(msg); } catch (TelegramApiException e) { throw new RuntimeException(e); }
                return true;
            } else if ("awaiting_flight_edit_departure_id".equals(state)) {
                try {
                    Long airportId = Long.parseLong(text);
                    var depAirportOpt = airportRepository.findById(airportId);
                    if (depAirportOpt.isEmpty()) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Аэропорт с ID " + airportId + " не найден. Введите корректный ID:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }
                    Flight flight = tempFlights.get(chatId);
                    flight.setDepartureAirport(depAirportOpt.get());
                    userStates.put(chatId, "awaiting_flight_edit_arrival_id");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите новый ID аэропорта прибытия (текущий: " +
                                  (flight.getArrivalAirport() != null ? flight.getArrivalAirport().getId() : "-") + "):")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите корректный числовой ID аэропорта:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_flight_edit_arrival_id".equals(state)) {
                try {
                    Long airportId = Long.parseLong(text);
                    var arrAirportOpt = airportRepository.findById(airportId);
                    if (arrAirportOpt.isEmpty()) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Аэропорт с ID " + airportId + " не найден. Введите корректный ID:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }
                    Flight flight = tempFlights.get(chatId);
                    flight.setArrivalAirport(arrAirportOpt.get());
                    userStates.put(chatId, "awaiting_flight_edit_departure_time");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите новое время отправления (текущее: " +
                                  (flight.getDepartureTime() != null ? flight.getDepartureTime().toString().replace('T', ' ') : "-") +
                                  ") в формате 2025-05-20 10:00:")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите корректный числовой ID аэропорта:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_flight_edit_departure_time".equals(state)) {
                try {
                    java.time.LocalDateTime depTime = java.time.LocalDateTime.parse(text.replace(" ", "T"));
                    Flight flight = tempFlights.get(chatId);
                    flight.setDepartureTime(depTime);
                    userStates.put(chatId, "awaiting_flight_edit_arrival_time");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Введите новое время прибытия (текущее: " +
                                  (flight.getArrivalTime() != null ? flight.getArrivalTime().toString().replace('T', ' ') : "-") +
                                  ") в формате 2025-05-20 12:00:")
                            .build();
                    bot.execute(msg);
                } catch (Exception e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите дату и время в корректном формате (например, 2025-05-20 10:00):")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_flight_edit_arrival_time".equals(state)) {
                try {
                    java.time.LocalDateTime arrTime = java.time.LocalDateTime.parse(text.replace(" ", "T"));
                    Flight flight = tempFlights.get(chatId);
                    flight.setArrivalTime(arrTime);
                    flightRepository.save(flight);
                    userStates.remove(chatId);
                    tempFlights.remove(chatId);
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Рейс успешно обновлен!")
                            .build();
                    bot.execute(msg);
                } catch (Exception e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Пожалуйста, введите дату и время в корректном формате (например, 2025-05-20 12:00):")
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

