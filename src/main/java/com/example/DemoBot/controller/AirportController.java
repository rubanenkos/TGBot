package com.example.DemoBot.controller;

import java.util.List;
import com.example.DemoBot.model.Airport;
import com.example.DemoBot.service.AirportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/airports")
public class AirportController {

    private final AirportService airportService;

    @Autowired
    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @GetMapping("/all")
    public List<Airport> getAllAirports() {
        return airportService.getAllAirports();
    }

    @PostMapping("/create")
    public Airport createAirport(@RequestBody Airport airport) {
        return airportService.createAirport(airport);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteAirport(@PathVariable Long id) {
        airportService.deleteAirport(id);
    }
}