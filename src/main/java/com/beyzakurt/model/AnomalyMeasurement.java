package com.beyzakurt.model;

import com.influxdb.annotations.Measurement;
import lombok.Data;

@Data
@Measurement(name = "air_anomalies")
public class AnomalyMeasurement {
    private String pollutant;
    private Double currentValue;
    private Double thresholdValue;
    private String detectionMethod;
    private String description;
}
