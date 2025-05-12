package com.beyzakurt.service;

import com.beyzakurt.config.ExternalApiProperties;
import com.beyzakurt.model.AirPollution;
import com.influxdb.client.InfluxDBClient;

import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@Service
public class AirPollutionService {

    private final WebClient webClient;
    private final InfluxDBClient influxDBClient;
    private final ExternalApiProperties externalApiProperties;
    private final KafkaProducerService kafkaProducerService;


    public Mono<AirPollution> getAirPollutionData(String city) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/feed/{city}/")
                        .queryParam("token", externalApiProperties.getToken())
                        .build(city))
                .retrieve()
                .bodyToMono(AirPollution.class)
                .doOnNext(kafkaProducerService::sendAirPollutionData);
                //.doOnNext(this::saveToInfluxDb);
    }


    private void saveToInfluxDb(AirPollution response) {
        if (response == null || response.getData() == null)
            return;

        AirPollution.AirPollutionData data = response.getData();

        // Point oluşturma (InfluxDB veri noktası)
        Point point = Point.measurement("air_quality")
                .addTag("city", data.getCity().getName())
                .addTag("location", data.getCity().getGeo().get(0) + "," + data.getCity().getGeo().get(1))
                .addField("aqi", data.getAqi())
                .addField("pm25", data.getIaqi().getPm25().getV())
                .addField("pm10", data.getIaqi().getPm10().getV())
                .addField("co", data.getIaqi().getCo().getV())
                .addField("no2", data.getIaqi().getNo2().getV())
                .addField("o3", data.getIaqi().getO3().getV())
                .addField("so2", data.getIaqi().getSo2().getV())
                .time(Instant.ofEpochSecond(data.getTime().getV()), WritePrecision.S);

        // InfluxDB'ye yazma
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(point);
        }
    }


    public Mono<String> saveAirPollutionData(AirPollution response) {
        AirPollution.AirPollutionData data =  response.getData();

        return Mono.fromCallable(() -> {
            Point point = Point.measurement("air_quality")
                    .addTag("city", data.getCity().getName())
                    .addTag("location", data.getCity().getLat() + "," + data.getCity().getLon())
                    .addField("aqi", data.getAqi())
                    .addField("pm25", data.getIaqi().getPm25().getV())
                    .addField("pm10", data.getIaqi().getPm10().getV())
                    .addField("co", data.getIaqi().getCo().getV())
                    .addField("no2", data.getIaqi().getNo2().getV())
                    .addField("o3", data.getIaqi().getO3().getV())
                    .addField("so2", data.getIaqi().getSo2().getV())
                    .time(Instant.ofEpochSecond(data.getTime().getV()), WritePrecision.NS);

            try (WriteApi writeApi = influxDBClient.getWriteApi()) {
                writeApi.writePoint(point);
                writeApi.flush();
            }
            return "Veri başarıyla kaydedildi: " + data.getCity();
        });
    }

}

