package com.beyzakurt.service;

import com.beyzakurt.model.AirPollution;
import com.beyzakurt.model.Anomaly;
import com.beyzakurt.model.AnomalyAlert;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.influxdb.client.InfluxDBClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final InfluxDBClient influxDBClient;
    private final KafkaProducerService kafkaProducerService;

    // WHO standards
    private static final Map<String, Double> WHO_THRESHOLDS = Map.of(
            "pm25", 25.0,
            "pm10", 50.0,
            "co", 10.0,
            "no2", 25.0,
            "o3", 100.0,
            "so2", 20.0
    );


    public Flux<Anomaly> getHistoricalAnomalies(String city, Instant start, Instant end) {
        return Flux.fromIterable(queryAnomaliesByTimeRange(city, start, end))
                .doOnSubscribe(s -> log.info("Tarihsel anomali sorgusu başladı: {} - {} -> {}", city, start, end))
                .doOnComplete(() -> log.info("Tarihsel anomali sorgusu tamamlandı"));
    }


    private List<Anomaly> queryAnomaliesByTimeRange(String city, Instant start, Instant end) {
        List<Anomaly> anomalies = new ArrayList<>();

        String fluxQuery = String.format(
                "from(bucket: \"AirPollutionData\")\n" +
                        "  |> range(start: %s, stop: %s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"air_anomalies\")\n" +
                        "  |> filter(fn: (r) => r.city =~ /(?i)%s/)\n" +
                        "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n" +
                        "  |> keep(columns: [\"_time\", \"city\", \"pollutant\", \"detection_method\", \"current_value\", \"threshold_value\", \"description\"])\n",
                start.toString(),
                end.toString(),
                city.toLowerCase()
        );

        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Anomaly anomaly = new Anomaly();

                    Object pollutant = record.getValueByKey("pollutant");
                    if (pollutant != null) anomaly.setPollutant(pollutant.toString());

                    Object currentVal = record.getValueByKey("current_value");
                    if (currentVal instanceof Double) anomaly.setCurrentValue((Double) currentVal);

                    Object thresholdVal = record.getValueByKey("threshold_value");
                    if (thresholdVal instanceof Double) anomaly.setThresholdValue((Double) thresholdVal);

                    Object method = record.getValueByKey("detection_method");
                    if (method != null) anomaly.setDetectionMethod(method.toString());

                    Object desc = record.getValueByKey("description");
                    if (desc != null) anomaly.setDescription(desc.toString());

                    anomalies.add(anomaly);
                }
            }
        } catch (Exception e) {
            log.error("Tarihsel anomali sorgulama hatası: {}", e.getMessage());
        }

        return anomalies;
    }



    // Anomali kaydetme metodu ekle (detectAndNotifyAnomalies içinde kullanılıyor)
    private void saveAnomalyToInflux(Anomaly anomaly, String city, Instant timestamp) {
        Point point = Point.measurement("air_anomalies")
                .addTag("city",city)
                .addTag("pollutant", anomaly.getPollutant())
                .addTag("detection_method", anomaly.getDetectionMethod())
                .addField("current_value", anomaly.getCurrentValue())
                .addField("threshold_value", anomaly.getThresholdValue())
                .addField("description", anomaly.getDescription())
                .time(timestamp, WritePrecision.NS);

        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(point);
            log.info("Anomali InfluxDB'ye kaydedildi: {}", anomaly);
        } catch (Exception e) {
            log.error("Anomali InfluxDB'ye kaydedilirken hata: {}", e.getMessage());
        }
    }


    // Anomali tespiti yap ve Kafka'ya gönder
    public void detectAndNotifyAnomalies(AirPollution data) {
        if (data == null || data.getData() == null) return;

        String city = data.getData().getCity().getName();
        Instant timestamp = Instant.ofEpochSecond(data.getData().getTime().getV());
        List<Anomaly> anomalies = detectAnomalies(data);

        if (!anomalies.isEmpty()) {
            // InfluxDB'ye kaydet
            anomalies.forEach(anomaly -> saveAnomalyToInflux(anomaly, city, timestamp));

            // Kafka'ya gönder
            kafkaProducerService.sendAnomalyAlert(new AnomalyAlert(
                    city,
                    timestamp,
                    anomalies));
        }
    }



    // Anomali tespit yöntemlerini uygula
    public List<Anomaly> detectAnomalies(AirPollution data) {
        List<Anomaly> anomalies = new ArrayList<>();

        // WHO threshold kontrolü
        anomalies.addAll(checkWhoThresholds(data));

        // 24 saatlik ortalama kontrolü
        anomalies.addAll(check24HourAverage(data));

        // Bölgesel farklılık kontrolü
        anomalies.addAll(checkRegionalDifferences(data));

        // İstatistiksel anomali kontrolü (Z-score ve IQR)
        anomalies.addAll(checkStatisticalAnomalies(data));

        return anomalies;
    }



    // WHO standartlarına göre anomali kontrolü
    private List<Anomaly> checkWhoThresholds(AirPollution data) {
        List<Anomaly> anomalies = new ArrayList<>();
        AirPollution.AirPollutionData.IAQI iaqi = data.getData().getIaqi();

        WHO_THRESHOLDS.forEach((pollutant, threshold) -> {
            try {
                Double value = getPollutantValue(iaqi, pollutant);
                if (value != null && value > threshold) {
                    anomalies.add(new Anomaly(
                            pollutant,
                            value,
                            threshold,
                            "WHO_THRESHOLD",
                            String.format("%s seviyesi WHO standartlarını aştı (%.2f > %.2f)",
                                    pollutant, value, threshold)
                    ));
                }
            } catch (Exception e) {
                log.error("WHO threshold kontrolü sırasında hata: {}", e.getMessage());
            }
        });

        return anomalies;
    }


    // 24 saatlik ortalamaya göre anomali kontrolü
    private List<Anomaly> check24HourAverage(AirPollution data) {
        List<Anomaly> anomalies = new ArrayList<>();
        String city = data.getData().getCity().getName();
        Instant now = Instant.ofEpochSecond(data.getData().getTime().getV());
        Instant start = now.minus(24, ChronoUnit.HOURS);

        // InfluxDB'den son 24 saatlik verileri al
        String fluxQuery = String.format(
                "from(bucket: \"AirPollutionData\")\n" +
                        "  |> range(start: %s, stop: %s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"air_quality\")\n" +
                        "  |> filter(fn: (r) => r.city == \"%s\")",
                start.toString(), now.toString(), city
        );

        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            // Her bir kirletici için ortalama hesapla
            for (String pollutant : WHO_THRESHOLDS.keySet()) {
                String query = fluxQuery +
                        String.format("\n  |> filter(fn: (r) => r._field == \"%s\")", pollutant) +
                        "\n  |> mean()";

                List<FluxTable> tables = queryApi.query(query);
                if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
                    double avg = (double) tables.get(0).getRecords().get(0).getValue();
                    double current = getPollutantValue(data.getData().getIaqi(), pollutant);

                    if (current > avg * 1.5) { // %50'den fazla artış
                        anomalies.add(new Anomaly(
                                pollutant,
                                current,
                                avg,
                                "24H_AVERAGE",
                                String.format("%s seviyesi son 24 saatlik ortalamanın %%50 üzerinde (%.2f > %.2f)",
                                        pollutant, current, avg)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.error("24 saatlik ortalama kontrolü sırasında hata: {}", e.getMessage());
        }

        return anomalies;
    }


    private List<Anomaly> checkRegionalDifferences(AirPollution data) {
        List<Anomaly> anomalies = new ArrayList<>();

        Double lat = null;
        Double lon = null;
        try {
            if (data.getData().getCity().getGeo() != null && data.getData().getCity().getGeo().size() >= 2) {
                lat = data.getData().getCity().getGeo().get(0);
                lon = data.getData().getCity().getGeo().get(1);
            }
        } catch (Exception e) {
            log.error("Koordinatlar alınırken hata: {}", e.getMessage());
        }

        if (lat == null || lon == null || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            log.warn("Geçersiz koordinatlar - lat: {}, lon: {}", lat, lon);
            return anomalies;
        }

        Instant now = Instant.ofEpochSecond(data.getData().getTime().getV());
        Instant start = now.minus(1, ChronoUnit.HOURS);
        QueryApi queryApi = influxDBClient.getQueryApi();

        for (String pollutant : WHO_THRESHOLDS.keySet()) {
            try {
                String fluxQuery = String.format(
                        "from(bucket: \"AirPollutionData\")\n" +
                                "  |> range(start: %s, stop: %s)\n" +
                                "  |> filter(fn: (r) => r._measurement == \"air_quality\")\n" +
                                "  |> filter(fn: (r) => r._field == \"%s\")\n" +
                                "  |> keep(columns: [\"_time\", \"_value\", \"lat\", \"lon\"])",
                        start.toString(), now.toString(), pollutant
                );

                List<FluxTable> tables = queryApi.query(fluxQuery);
                List<Double> nearbyValues = new ArrayList<>();

                for (FluxTable table : tables) {
                    for (FluxRecord record : table.getRecords()) {
                        Object latObj = record.getValueByKey("lat");
                        Object lonObj = record.getValueByKey("lon");

                        if (latObj instanceof Number && lonObj instanceof Number) {
                            double recLat = ((Number) latObj).doubleValue();
                            double recLon = ((Number) lonObj).doubleValue();
                            double value = ((Number) record.getValue()).doubleValue();

                            double distance = haversine(lat, lon, recLat, recLon);
                            if (distance <= 25.0) {
                                nearbyValues.add(value);
                            }
                        }
                    }
                }

                if (!nearbyValues.isEmpty()) {
                    double regionalAvg = nearbyValues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                    double current = getPollutantValue(data.getData().getIaqi(), pollutant);

                    if (!Double.isNaN(current) && Math.abs(current - regionalAvg) > regionalAvg * 0.5) {
                        anomalies.add(new Anomaly(
                                pollutant,
                                current,
                                regionalAvg,
                                "REGIONAL_DIFFERENCE",
                                String.format("%s seviyesi bölge ortalamasından %%50 farklı (%.2f vs %.2f)",
                                        pollutant, current, regionalAvg)
                        ));
                    }
                }
            } catch (Exception e) {
                log.error("Bölgesel farklılık kontrolü sırasında hata ({}): {}", pollutant, e.getMessage());
            }
        }

        return anomalies;
    }


    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Dünya yarıçapı (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }



    // İstatistiksel anomali tespiti (Z-score ve IQR)
    private List<Anomaly> checkStatisticalAnomalies(AirPollution data) {
        List<Anomaly> anomalies = new ArrayList<>();
        String city = data.getData().getCity().getName();
        Instant now = Instant.ofEpochSecond(data.getData().getTime().getV());
        Instant start = now.minus(7, ChronoUnit.DAYS); // 7 günlük veri

        String baseQuery = String.format(
                "from(bucket: \"AirPollutionData\")\n" +
                        "  |> range(start: %s, stop: %s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"air_quality\")\n" +
                        "  |> filter(fn: (r) => r.city == \"%s\")",
                start.toString(), now.toString(), city
        );


        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            for (String pollutant : WHO_THRESHOLDS.keySet()) {
                // Z-score hesapla
                String statsQuery = baseQuery +
                        String.format("\n  |> filter(fn: (r) => r._field == \"%s\")", pollutant) +
                        "\n  |> mean()\n" +
                        "  |> yield(name: \"mean\")\n\n" +
                        baseQuery +
                        String.format("\n  |> filter(fn: (r) => r._field == \"%s\")", pollutant) +
                        "\n  |> stddev()\n" +
                        "  |> yield(name: \"stddev\")";

                List<FluxTable> statsTables = queryApi.query(statsQuery);
                if (statsTables.size() >= 2) {
                    double mean = (double) statsTables.get(0).getRecords().get(0).getValue();
                    double stddev = (double) statsTables.get(1).getRecords().get(0).getValue();
                    double current = getPollutantValue(data.getData().getIaqi(), pollutant);

                    if (stddev > 0) { // Sıfır bölme hatasını önle
                        double zScore = Math.abs((current - mean) / stddev);
                        if (zScore > 3) { // 3 sigma kuralı
                            anomalies.add(new Anomaly(
                                    pollutant,
                                    current,
                                    mean,
                                    "Z_SCORE",
                                    String.format("%s seviyesi Z-score anomali (%.2f, mean=%.2f, stddev=%.2f)",
                                            pollutant, current, mean, stddev)
                            ));
                        }
                    }
                }

                // IQR (Interquartile Range) hesapla
                String iqrQuery = baseQuery +
                        String.format("\n  |> filter(fn: (r) => r._field == \"%s\")", pollutant) +
                        "\n  |> quantile(q: 0.25)\n" +
                        "  |> yield(name: \"q1\")\n\n" +
                        baseQuery +
                        String.format("\n  |> filter(fn: (r) => r._field == \"%s\")", pollutant) +
                        "\n  |> quantile(q: 0.75)\n" +
                        "  |> yield(name: \"q3\")";

                List<FluxTable> iqrTables = queryApi.query(iqrQuery);
                if (iqrTables.size() >= 2) {
                    double q1 = (double) iqrTables.get(0).getRecords().get(0).getValue();
                    double q3 = (double) iqrTables.get(1).getRecords().get(0).getValue();
                    double iqr = q3 - q1;
                    double current = getPollutantValue(data.getData().getIaqi(), pollutant);

                    if (current < q1 - 1.5 * iqr || current > q3 + 1.5 * iqr) {
                        anomalies.add(new Anomaly(
                                pollutant,
                                current,
                                q3 + 1.5 * iqr,
                                "IQR",
                                String.format("%s seviyesi IQR anomali (%.2f, Q1=%.2f, Q3=%.2f)",
                                        pollutant, current, q1, q3)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.error("İstatistiksel anomali kontrolü sırasında hata: {}", e.getMessage());
        }

        return anomalies;
    }


    // Kirletici değerini almak için yardımcı metod
    private Double getPollutantValue(AirPollution.AirPollutionData.IAQI iaqi, String pollutant) {
        try {
            switch (pollutant) {
                case "pm25": return iaqi.getPm25() != null ? iaqi.getPm25().getV() : null;
                case "pm10": return iaqi.getPm10() != null ? iaqi.getPm10().getV() : null;
                case "co": return iaqi.getCo() != null ? iaqi.getCo().getV() : null;
                case "no2": return iaqi.getNo2() != null ? iaqi.getNo2().getV() : null;
                case "o3": return iaqi.getO3() != null ? iaqi.getO3().getV() : null;
                case "so2": return iaqi.getSo2() != null ? iaqi.getSo2().getV() : null;
                default: return null;
            }
        } catch (Exception e) {
            log.warn("Kirletici verisi alınamadı: {}", pollutant);
            return null;
        }
    }

}
