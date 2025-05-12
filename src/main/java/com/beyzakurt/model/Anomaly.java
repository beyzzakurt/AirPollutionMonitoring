package com.beyzakurt.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {
    private String pollutant; // pm25, pm10, co, no2, o3, so2
    private Double currentValue;
    private Double thresholdValue;
    private String detectionMethod; // WHO_THRESHOLD, 24H_AVERAGE, REGIONAL_DIFFERENCE, Z_SCORE, IQR
    private String description;
}