package com.beyzakurt.service;

import com.beyzakurt.model.AirPollutionMessage;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final InfluxDBClient influxDBClient;

    @KafkaListener(topics = "${spring.kafka.topic.air-pollution}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAirPollutionData(AirPollutionMessage message){
        log.info("Kafka'dan hava kirliliği verisi alındı: {}", message);

        Point point = Point.measurement("air_quality")
                .addTag("city", message.getCity())
                .addField("aqi", message.getAqi())
                .addField("pm25", message.getPm25())
                .addField("pm10", message.getPm10())
                .addField("co", message.getCo())
                .addField("no2", message.getNo2())
                .addField("o3", message.getO3())
                .addField("so2", message.getSo2())
                .time(message.getTimestamp(), WritePrecision.NS);

        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(point);
            log.info("Veri InfluxDB'ye kaydedildi: {}", message.getCity());
        } catch (Exception e) {
            log.error("InfluxDB'ye yazma hatası: {}", e.getMessage());
        }

    }
}



