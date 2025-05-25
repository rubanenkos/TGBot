package com.example.DemoBot.service;


import com.example.DemoBot.model.Flight;
import java.util.List;

public interface FlightService {

    List<Flight> getAllFlights();

    Flight createFlight(Flight flight);

    void deleteFlight(Long id);
}
