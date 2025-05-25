package com.example.DemoBot.bot;

import com.example.DemoBot.model.Ticket;
import com.example.DemoBot.model.Flight;
import com.example.DemoBot.repository.TicketRepository;
import com.example.DemoBot.repository.FlightRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TicketCallbackHandler {
    private final TicketRepository ticketRepository;
    private final FlightRepository flightRepository;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Ticket> tempTickets = new HashMap<>();

    public TicketCallbackHandler(TicketRepository ticketRepository, FlightRepository flightRepository) {
        this.ticketRepository = ticketRepository;
        this.flightRepository = flightRepository;
    }

    public boolean handleCallback(String callbackData, CallbackQuery callbackQuery, MyTelegramBot bot) {
        switch (callbackData) {
            case "ticket_show_all" -> {
                handleShowAll(callbackQuery, bot);
                return true;
            }
            case "ticket_add" -> {
                handleAdd(callbackQuery, bot);
                return true;
            }
            case "ticket_edit" -> {
                handleEdit(callbackQuery, bot);
                return true;
            }
            case "ticket_delete" -> {
                handleDelete(callbackQuery, bot);
                return true;
            }
        }
        return false;
    }

    public boolean handleTicketMessage(Update update, MyTelegramBot bot) {
        // Здесь можно реализовать пошаговый ввод для создания, удаления билета
        // Аналогично FlightCallbackHandler
        return false;
    }

    private void handleShowAll(CallbackQuery callbackQuery, MyTelegramBot bot) {
        List<Ticket> tickets = ticketRepository.findAll();
        StringBuilder sb = new StringBuilder();
        if (tickets.isEmpty()) {
            sb.append("Билеты не найдены.");
        } else {
            sb.append("Список билетов:\n");
            for (Ticket t : tickets) {
                sb.append("ID: ").append(t.getId())
                  .append(", Рейс: ").append(t.getFlight() != null ? t.getFlight().getFlightNumber() : "-")
                  .append(", UserID: ").append(t.getUserId())
                  .append(", Тип: ").append(t.getTicketType())
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
        userStates.put(chatId, "awaiting_ticket_flight_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Введите ID рейса для билета:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEdit(CallbackQuery callbackQuery, MyTelegramBot bot) {

    }

    private void handleDelete(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_ticket_delete_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Введите ID билета для удаления:")
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
    public Ticket getTempTicket(Long chatId) {
        return tempTickets.get(chatId);
    }
    public void setTempTicket(Long chatId, Ticket ticket) {
        tempTickets.put(chatId, ticket);
    }
    public void removeTempTicket(Long chatId) {
        tempTickets.remove(chatId);
    }
    public TicketRepository getTicketRepository() {
        return ticketRepository;
    }
    public FlightRepository getFlightRepository() {
        return flightRepository;
    }
}

