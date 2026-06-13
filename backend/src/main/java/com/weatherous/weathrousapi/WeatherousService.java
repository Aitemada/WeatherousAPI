package com.weatherous.weathrousapi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.weatherous.weathrousapi.DtoRecords.DailyWeather;
import com.weatherous.weathrousapi.DtoRecords.ForecastResponse;
import com.weatherous.weathrousapi.DtoRecords.PastcastResponse;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class WeatherousService {

        private final RestClient restClient;
        private final StringRedisTemplate redisTemplate;
        private final ObjectMapper objectMapper;

        @Value("${weatherapi.key:fallbackkeyidonthave}")
        private String apiKey;

        private static final String CACHE_LIST_KEY = "recent_cities_list";
        private static final int MAX_CACHE_SIZE = 2;

        public WeatherousService(RestClient.Builder restClientBuilder, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
            this.restClient = restClientBuilder.baseUrl("https://api.weatherapi.com/v1").build();
            this.redisTemplate = redisTemplate;
            this.objectMapper = objectMapper;
        }

        public ForecastResponse getForecast(String city) {
            String cacheKey = "forecast:" + city.toLowerCase();

            try {
                // 1. Check Redis 
                String cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    updateLruList(cacheKey);
                    return objectMapper.readValue(cachedData, ForecastResponse.class);
                }

                // 2. Fetch from WeatherAPI
                JsonNode rootNode = restClient.get()
                        .uri("/forecast.json?key={key}&q={city}&days=7", apiKey, city)
                        .retrieve()
                        .body(JsonNode.class);

                // 3. Parse the JSON Tree
                List<DailyWeather> days = new ArrayList<>();
                JsonNode forecastDays = rootNode.path("forecast").path("forecastday");

                for (JsonNode dayNode : forecastDays) {
                    String date = dayNode.path("date").asText();
                    JsonNode dayData = dayNode.path("day");

                    double temp = dayData.path("avgtemp_c").asDouble();
                    String condition = dayData.path("condition").path("text").asText();
                    double humidity = dayData.path("avghumidity").asDouble();
                    double windSpeed = dayData.path("maxwind_kph").asDouble();

                    days.add(new DailyWeather(date, temp, condition, humidity, windSpeed));
                }

                ForecastResponse response = new ForecastResponse(city, days);

                // 4. Save to cache
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), 1, TimeUnit.HOURS);
                enforceLruCacheLimit(cacheKey);

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch forecast from WeatherAPI", e);
            }
        }

        public PastcastResponse getPastcast(String city, String date) {
            String cacheKey = "pastcast:" + city.toLowerCase() + ":" + date;

            try {
                // 1. Check Redis 
                String cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    updateLruList(cacheKey);
                    return objectMapper.readValue(cachedData, PastcastResponse.class);
                }

                // 2. Fetch from WeatherAPI
                JsonNode rootNode = restClient.get()
                        .uri("/history.json?key={key}&q={city}&dt={date}", apiKey, city, date)
                        .retrieve()
                        .body(JsonNode.class);

                // 3. Parse the JSON Tree
                JsonNode targetDayNode = rootNode.path("forecast").path("forecastday").get(0);

                double temp = targetDayNode.path("day").path("avgtemp_c").asDouble();
                String condition = targetDayNode.path("day").path("condition").path("text").asText();
                String moonPhase = targetDayNode.path("astro").path("moon_phase").asText();

                PastcastResponse response = new PastcastResponse(city, date, temp, condition, moonPhase);

                // 4. Save to cache
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), 30, TimeUnit.DAYS); // History doesn't change, cache longer
                enforceLruCacheLimit(cacheKey);

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch historical data from WeatherAPI", e);
            }
        }

        private void enforceLruCacheLimit(String newCacheKey) {
            redisTemplate.opsForList().leftPush(CACHE_LIST_KEY, newCacheKey);

            Long size = redisTemplate.opsForList().size(CACHE_LIST_KEY);
            if (size != null && size > MAX_CACHE_SIZE) {
                String oldestKey = redisTemplate.opsForList().rightPop(CACHE_LIST_KEY);
                if (oldestKey != null) {
                    redisTemplate.delete(oldestKey);
                }
            }
        }

        private void updateLruList(String existingKey) {
            redisTemplate.opsForList().remove(CACHE_LIST_KEY, 0, existingKey);
            redisTemplate.opsForList().leftPush(CACHE_LIST_KEY, existingKey);
        }
}
