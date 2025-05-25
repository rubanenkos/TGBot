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
        // Здесь можно реализовать пошаговый ввод для создания, удаления, редактирования рейса
        // Аналогично AirportCallbackHandler
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
        // Аналогично AirportCallbackHandler: реализовать пошаговый ввод данных для создания рейса
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

