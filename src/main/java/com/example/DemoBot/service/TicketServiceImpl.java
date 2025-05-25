package com.example.DemoBot.service;

import com.example.DemoBot.model.Ticket;
import com.example.DemoBot.repository.TicketRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Override
    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Override
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
    }

    @Override
    public Ticket updateTicket(Ticket ticket) {
        if (ticket.getId() == null || !ticketRepository.existsById(ticket.getId())) {
            throw new IllegalArgumentException("Ticket with this ID not found");
        }
        return ticketRepository.save(ticket);
    }
}
