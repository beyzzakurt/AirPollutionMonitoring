package com.beyzakurt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external.api")
public class ExternalApiProperties {

    private String url;
    private String token;
}
