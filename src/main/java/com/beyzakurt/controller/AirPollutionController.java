package com.beyzakurt.controller;

import com.beyzakurt.model.AirPollutionModel;
import com.beyzakurt.service.AirPollutionService;
import com.beyzakurt.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rest/api/air-pollution")
@RequiredArgsConstructor
public class AirPollutionController {

    private final AirPollutionService airPollutionService;
    private final KafkaProducerService kafkaProducerService;

    @GetMapping("/{city}")
    public Mono<AirPollutionModel> getAirPollution(@PathVariable String city) {
        return airPollutionService.getAirPollutionData(city);
    }


    @PostMapping("/add")
    public Mono<String> addAirQualityData(@RequestBody AirPollutionModel data) {
        return airPollutionService.saveAirPollutionData(data);
    }

    @PostMapping("/send-to-kafka")
    public Mono<String> sendToKafka(@RequestBody AirPollutionModel data) {
        kafkaProducerService.sendAirPollutionData(data);
        return Mono.just("Veri Kafka'ya gönderildi: " + data.getData().getCity().getName());
    }

}
