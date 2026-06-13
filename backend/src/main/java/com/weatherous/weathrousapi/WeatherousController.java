package com.weatherous.weathrousapi;

import com.weatherous.weathrousapi.DtoRecords.*;
import com.weatherous.weathrousapi.WeatherousService.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherousController {

    private final WeatherousService weatherService;

    public WeatherousController(WeatherousService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/forecast")
    public ResponseEntity<ForecastResponse> getForecast(@RequestParam String city) {
        return ResponseEntity.ok(weatherService.getForecast(city));
    }

    @GetMapping("/pastcast")
    public ResponseEntity<PastcastResponse> getPastcast(@RequestParam String city, @RequestParam String date) {
        return ResponseEntity.ok(weatherService.getPastcast(city, date));
    }
}