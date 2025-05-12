package com.beyzakurt.service;

import com.beyzakurt.model.AirPollutionMessage;
import com.beyzakurt.model.AirPollution;
import com.beyzakurt.model.AnomalyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.air-pollution}")
    private String airPollutionTopic;

    @Value("${spring.kafka.topic.anomaly-alerts}")
    private String anomalyAlertsTopic;

    public void sendAirPollutionData(AirPollution model) {
        if (model == null || model.getData() == null) {
            log.warn("Gönderilecek hava kirliliği verisi boş");
            return;
        }

        AirPollution.AirPollutionData data = model.getData();

        Double lat = data.getCity().getLat();
        Double lon = data.getCity().getLon();

        AirPollutionMessage message = AirPollutionMessage.builder()
                .city(data.getCity().getName())
                .timestamp(Instant.ofEpochSecond(data.getTime().getV()))
                .aqi(data.getAqi().intValue())
                .pm25(data.getIaqi().getPm25().getV())
                .pm10(data.getIaqi().getPm10().getV())
                .co(data.getIaqi().getCo().getV())
                .no2(data.getIaqi().getNo2().getV())
                .o3(data.getIaqi().getO3().getV())
                .so2(data.getIaqi().getSo2().getV())
                .lat(lat)
                .lon(lon)
                .build();


        kafkaTemplate.send(airPollutionTopic, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Hava kirliliği verisi Kafka'ya gönderildi: {}, Partition: {}, Offset: {}",
                                message,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Kafka'ya veri gönderilirken hata oluştu: {}", ex.toString());
                    }
                });
    }


    public void sendAnomalyAlert(AnomalyAlert alert) {
        kafkaTemplate.send(anomalyAlertsTopic, alert)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Anomali uyarısı Kafka'ya gönderildi: {}, Partition: {}, Offset: {}",
                                alert,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Anomali uyarısı Kafka'ya gönderilirken hata oluştu: {}", ex.toString());
                    }
                });
    }
}

