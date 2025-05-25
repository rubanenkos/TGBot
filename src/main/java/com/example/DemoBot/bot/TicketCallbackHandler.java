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
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            String state = userStates.get(chatId);
            if ("awaiting_ticket_flight_id".equals(state)) {
                try {
                    Long flightId = Long.parseLong(text);
                    var flightOpt = flightRepository.findById(flightId);
                    if (flightOpt.isEmpty()) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Flight with this ID not found. Please enter a valid flight ID:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }
                    Ticket ticket = new Ticket();
                    ticket.setFlight(flightOpt.get());
                    tempTickets.put(chatId, ticket);
                    userStates.put(chatId, "awaiting_ticket_user_id");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Enter userId (positive number only):")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter the numeric flight ID:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_ticket_user_id".equals(state)) {
                try {
                    long userId = Long.parseLong(text);
                    if (userId <= 0) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("userId must be a positive number. Enter userId:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }
                    Ticket ticket = tempTickets.get(chatId);
                    ticket.setUserId(userId);
                    userStates.put(chatId, "awaiting_ticket_type");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Enter the ticket type (e.g. ECONOMY, BUSINESS):")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter a numeric userId:")
                            .build();
                    try { bot.execute(msg); } catch (TelegramApiException ex) { throw new RuntimeException(ex); }
                }
                return true;
            } else if ("awaiting_ticket_type".equals(state)) {
                Ticket ticket = tempTickets.get(chatId);
                ticket.setTicketType(text);
                ticketRepository.save(ticket);
                userStates.remove(chatId);
                tempTickets.remove(chatId);
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Ticket added successfully!")
                        .build();
                try {
                    bot.execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return true;
            } else if ("awaiting_ticket_delete_id".equals(state)) {
                try {
                    Long id = Long.parseLong(text);
                    if (ticketRepository.existsById(id)) {
                        ticketRepository.deleteById(id);
                        userStates.remove(chatId);
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Ticket with ID " + id + " successfully deleted!")
                                .build();
                        try {
                            bot.execute(msg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Ticket with this ID not found. Please enter a valid ID:")
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
                            .text("Please enter the numeric ticket ID:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_ticket_edit_id".equals(state)) {
                try {
                    Long id = Long.parseLong(text);
                    var ticketOpt = ticketRepository.findById(id);
                    if (ticketOpt.isPresent()) {
                        Ticket ticket = ticketOpt.get();
                        tempTickets.put(chatId, ticket);
                        userStates.put(chatId, "awaiting_ticket_edit_flight_id");
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Please enter new flight ID (current: " +
                                      (ticket.getFlight() != null ? ticket.getFlight().getId() : "not set") + "):")
                                .build();
                        bot.execute(msg);
                    } else {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Ticket with ID " + id + " not found. Please enter a valid ID:")
                                .build();
                        bot.execute(msg);
                    }
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter a valid numeric ticket ID:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_ticket_edit_flight_id".equals(state)) {
                try {
                    Long flightId = Long.parseLong(text);
                    var flightOpt = flightRepository.findById(flightId);
                    if (flightOpt.isEmpty()) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Flight with ID " + flightId + " not found. Please enter a valid flight ID:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }

                    Ticket ticket = tempTickets.get(chatId);
                    ticket.setFlight(flightOpt.get());
                    userStates.put(chatId, "awaiting_ticket_edit_user_id");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Enter new userId (current: " + ticket.getUserId() + "):")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter a valid numeric flight ID:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_ticket_edit_user_id".equals(state)) {
                try {
                    Long userId = Long.parseLong(text);
                    if (userId <= 0) {
                        SendMessage msg = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("UserId must be a positive number. Please enter a valid value:")
                                .build();
                        bot.execute(msg);
                        return true;
                    }

                    Ticket ticket = tempTickets.get(chatId);
                    ticket.setUserId(userId);
                    userStates.put(chatId, "awaiting_ticket_edit_type");
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Enter a new ticket type (e.g. ECONOMY or BUSINESS) (current: " + ticket.getTicketType() + "):")
                            .build();
                    bot.execute(msg);
                } catch (NumberFormatException | TelegramApiException e) {
                    SendMessage msg = SendMessage.builder()
                            .chatId(String.valueOf(chatId))
                            .text("Please enter a valid numeric value for userId:")
                            .build();
                    try {
                        bot.execute(msg);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return true;
            } else if ("awaiting_ticket_edit_type".equals(state)) {
                Ticket ticket = tempTickets.get(chatId);
                ticket.setTicketType(text.toUpperCase());
                ticketRepository.save(ticket);
                userStates.remove(chatId);
                tempTickets.remove(chatId);
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Ticket successfully updated!")
                        .build();
                try {
                    bot.execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }
        return false;
    }

    private void handleShowAll(CallbackQuery callbackQuery, MyTelegramBot bot) {
        List<Ticket> tickets = ticketRepository.findAll();
        StringBuilder sb = new StringBuilder();
        if (tickets.isEmpty()) {
            sb.append("Tickets not found.");
        } else {
            sb.append("List of tickets:\n");
            for (Ticket t : tickets) {
                sb.append("ID: ").append(t.getId())
                  .append(", Route: ").append(t.getFlight() != null ? t.getFlight().getFlightNumber() : "-")
                  .append(", UserID: ").append(t.getUserId())
                  .append(", Type: ").append(t.getTicketType())
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
                .text("Enter flight ID for ticket:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEdit(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_ticket_edit_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Enter ticket ID to edit:")
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDelete(CallbackQuery callbackQuery, MyTelegramBot bot) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userStates.put(chatId, "awaiting_ticket_delete_id");
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Enter the ticket ID to delete:")
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

