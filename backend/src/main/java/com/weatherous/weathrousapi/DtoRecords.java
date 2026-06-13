package com.weatherous.weathrousapi;
import java.util.List;

public class DtoRecords {
    public record WeatherRequest(String country, String city, String date, String mode) {}

    public record DailyWeather(String date, double temp, String condition, double humidity, double windSpeed) {}

    public record ForecastResponse(String city, List<DailyWeather> days) {}

    public record PastcastResponse(String city, String date, double temp, String condition, String moonPhase) {}
}
