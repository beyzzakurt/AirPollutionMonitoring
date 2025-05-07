package com.beyzakurt.service;

import com.beyzakurt.model.AirPollutionMessage;
import com.beyzakurt.model.AirPollutionModel;
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
    private KafkaTemplate<String, AirPollutionMessage> kafkaTemplate;

    @Value("${spring.kafka.topic.air-pollution}")
    private String airPollutionTopic;

    public void sendAirPollutionData(AirPollutionModel model) {
        if (model == null || model.getData() == null) {
            log.warn("Gönderilecek hava kirliliği verisi boş");
            return;
        }

        AirPollutionModel.AirPollutionData data = model.getData();

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
}

