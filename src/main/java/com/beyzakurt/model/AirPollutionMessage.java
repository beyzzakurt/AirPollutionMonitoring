package com.beyzakurt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirPollutionMessage {

    private String city;
    private Instant timestamp;
    private Integer aqi;
    private Double pm25;
    private Double pm10;
    private Double co;
    private Double no2;
    private Double o3;
    private Double so2;
}
