package com.beyzakurt.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDbConfig {

    @Value("${influx.url}")
    private String url;

    @Value("${influx.token}")
    private String token;

    @Value("${influx.org}")
    private String org;

    @Value("${influx.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        WriteOptions writeOptions = WriteOptions.builder()
                .batchSize(500)           // 500 kayıtta bir gönder
                .flushInterval(1000)      // 1 saniyede bir gönder (ms)
                .jitterInterval(100)      // 100ms gecikme
                .retryInterval(5000)      // 5 saniyede bir yeniden dene
                .bufferLimit(10000)       // Maksimum 10,000 kayıt buffer'la
                .build();

        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket)
                .enableGzip();
    }

}
