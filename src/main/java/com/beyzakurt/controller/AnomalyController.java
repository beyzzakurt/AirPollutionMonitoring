package com.beyzakurt.controller;

import com.beyzakurt.model.Anomaly;
import com.beyzakurt.model.AnomalyAlert;
import com.beyzakurt.service.AirPollutionService;
import com.beyzakurt.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/rest/api/air-pollution/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AirPollutionService airPollutionService;
    private final AnomalyDetectionService anomalyDetectionService;

    // http://localhost:8080/rest/api/air-pollution/anomalies/lima
    @GetMapping("/{city}")
    public Mono<AnomalyAlert> getAirPollutionAnomalies(@PathVariable String city) {
        return airPollutionService.getAirPollutionData(city)
                .flatMap(data -> {
                    anomalyDetectionService.detectAndNotifyAnomalies(data);
                    return Mono.just(new AnomalyAlert(
                            data.getData().getCity().getName(),
                            Instant.ofEpochSecond(data.getData().getTime().getV()),
                            anomalyDetectionService.detectAnomalies(data)
                    ));
                });
    }

    // http://localhost:8080/rest/api/air-pollution/anomalies/history/lima?start=2025-05-04T01:00:00Z&end=2025-05-11T18:00:00Z
    @GetMapping("/history/{city}")
    public Flux<Anomaly> getHistoricalAnomalies(
            @PathVariable String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        return anomalyDetectionService.getHistoricalAnomalies(city, start, end);
    }
}