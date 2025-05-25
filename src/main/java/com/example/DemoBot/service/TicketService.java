package com.example.DemoBot.service;


import com.example.DemoBot.model.Ticket;
import java.util.List;

public interface TicketService {
    List<Ticket> getAllTickets();
    Ticket createTicket(Ticket ticket);
    void deleteTicket(Long id);
    Ticket updateTicket(Ticket ticket);
}
