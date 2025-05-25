package com.example.DemoBot.bot;

import com.example.DemoBot.constants.Actions;
import com.example.DemoBot.repository.AirportRepository;
import com.example.DemoBot.model.Airport;
import com.example.DemoBot.bot.FlightCallbackHandler;
import com.example.DemoBot.bot.TicketCallbackHandler;
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
    private final AirportCallbackHandler airportCallbackHandler;
    private final FlightCallbackHandler flightCallbackHandler;
    private final TicketCallbackHandler ticketCallbackHandler;

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Airport> tempAirports = new HashMap<>();

    @Autowired
    public MyTelegramBot(@Value("${bot.token}") String botToken,
                         @Value("${bot.username}") String username,
                         AirportRepository airportRepository,
                         AirportCallbackHandler airportCallbackHandler,
                         FlightCallbackHandler flightCallbackHandler,
                         TicketCallbackHandler ticketCallbackHandler) {
        super(botToken);
        this.username = username;
        this.airportRepository = airportRepository;
        this.airportCallbackHandler = airportCallbackHandler;
        this.flightCallbackHandler = flightCallbackHandler;
        this.ticketCallbackHandler = ticketCallbackHandler;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        System.out.println("Received update: " + update);
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            if ("/start".equals(text)) {
                InlineKeyboardMarkup mainMenu = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder().text("Airports").callbackData(Actions.TABLE_AIRPORT).build(),
                                InlineKeyboardButton.builder().text("Flights").callbackData(Actions.TABLE_FLIGHT).build(),
                                InlineKeyboardButton.builder().text("Tickets").callbackData(Actions.TABLE_TICKET).build()
                        ))
                        .build();
                SendMessage msg = SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Select a table to interact with:")
                        .replyMarkup(mainMenu)
                        .build();
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        if (update.hasCallbackQuery()){
            var callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();

            if (Actions.TABLE_AIRPORT.equals(callbackData)) {
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
                return;
            }

            if (Actions.TABLE_FLIGHT.equals(callbackData)) {
                InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Add Flight")
                                        .callbackData("flight_add")
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("Delete Flight")
                                        .callbackData("flight_delete")
                                        .build()))
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Edit Flight")
                                        .callbackData("flight_edit")
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("Show all Flights")
                                        .callbackData("flight_show_all")
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
                return;
            }

            if (Actions.TABLE_TICKET.equals(callbackData)) {
                InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Add Ticket")
                                        .callbackData("ticket_add")
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("Delete Ticket")
                                        .callbackData("ticket_delete")
                                        .build()))
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Edit Ticket")
                                        .callbackData("ticket_edit")
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("Show all Tickets")
                                        .callbackData("ticket_show_all")
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
                return;
            }

            if (airportCallbackHandler.handleCallback(callbackData, callbackQuery, this)) {
                try {
                    sendApiMethod(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text("Airport action performed")
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            if (flightCallbackHandler.handleCallback(callbackData, callbackQuery, this)) {
                try {
                    sendApiMethod(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text("Flight action performed")
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            if (ticketCallbackHandler.handleCallback(callbackData, callbackQuery, this)) {
                try {
                    sendApiMethod(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text("Ticket action performed")
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }

        if (airportCallbackHandler.handleAirportMessage(update, this)) {
            return;
        }
        if (flightCallbackHandler.handleFlightMessage(update, this)) {
            return;
        }
        if (ticketCallbackHandler.handleTicketMessage(update, this)) {
            return;
        }
    }
}
