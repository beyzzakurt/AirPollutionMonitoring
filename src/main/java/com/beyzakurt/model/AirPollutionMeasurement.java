package com.beyzakurt.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

@Data
@Measurement(name = "air_quality")
public class AirPollutionMeasurement {

    @Column(tag = true)
    private String city;

    @Column(tag = true)
    private String location; // "lat,lon"

    @Column
    private Double lat;

    @Column
    private Double lon;

    @Column
    private Integer aqi;

    @Column
    private Double pm25;

    @Column
    private Double pm10;

    @Column(name = "co")
    private Double carbonMonoxide;

    @Column(name = "no2")
    private Double nitrogenDioxide;

    @Column(name = "o3")
    private Double ozone;

    @Column(name = "so2")
    private Double sulphurDioxide;

    @Column(timestamp = true)
    private Instant time;


}