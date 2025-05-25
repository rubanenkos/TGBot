package com.example.DemoBot.service;


import java.util.List;
import com.example.DemoBot.model.Airport;
import com.example.DemoBot.repository.AirportRepository;
import org.springframework.stereotype.Service;

@Service
public class AirportServiceImpl implements AirportService {


    private final AirportRepository airportRepository;
    public AirportServiceImpl(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }


    @Override
    public List<Airport> getAllAirports() {
        return airportRepository.findAll();
    }

    @Override
    public Airport createAirport(Airport airport) {
        return airportRepository.save(airport);
    }

    @Override
    public void deleteAirport(Long id) {
        airportRepository.deleteById(id);
    }

    @Override
    public Airport updateAirport(Airport airport) {
        if (airport.getId() == null || !airportRepository.existsById(airport.getId())) {
            throw new IllegalArgumentException("Аэропорт с таким ID не найден");
        }
        return airportRepository.save(airport);
    }
}
