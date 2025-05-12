package com.beyzakurt.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AirPollution {

    private String status;
    private AirPollutionData data;

    @Data
    public static class AirPollutionData {
        private Integer aqi;
        private Integer idx;
        private List<Attribution> attributions;
        private City city;
        private String dominentpol;
        private Time time;
        private IAQI iaqi;

        @Data
        public static class Attribution {
            private String url;
            private String name;
            private String logo;
        }

        @Data
        public static class City {
            private String name;
            private String url;
            private List<Double> geo;

            public Double getLat() {
                return geo != null && geo.size() > 0 ? geo.get(0) : null;
            }

            public Double getLon() {
                return geo != null && geo.size() > 1 ? geo.get(1) : null;
            }
        }

        @Data
        public static class Time {
            private String s;
            private String tz;
            private Long v;
        }

        @Data
        public static class IAQI {
            @JsonProperty("co")
            private Value co; // Carbon Monoxide

            @JsonProperty("no2")
            private Value no2; // Nitrogen Dioxide

            @JsonProperty("o3")
            private Value o3; // Ozone

            @JsonProperty("pm10")
            private Value pm10; // PM10

            @JsonProperty("pm25")
            private Value pm25; // PM2.5

            @JsonProperty("so2")
            private Value so2; // Sulphur Dioxide

            @Data
            public static class Value {
                private Double v;
            }
        }
    }
}
