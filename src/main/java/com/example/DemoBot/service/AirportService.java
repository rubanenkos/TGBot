package com.example.DemoBot.service;

import java.util.List;
import com.example.DemoBot.model.Airport;

public interface AirportService {

    List<Airport> getAllAirports();

    Airport createAirport(Airport airport);

    void deleteAirport(Long id);

    Airport updateAirport(Airport airport);
}

