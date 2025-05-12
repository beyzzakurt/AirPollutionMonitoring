package com.beyzakurt.model;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyAlert {
    private String city;
    private Instant timestamp;
    private List<Anomaly> anomalies;
}