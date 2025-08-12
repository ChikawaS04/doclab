package com.doclab.doclab;

import com.doclab.doclab.config.NlpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NlpProperties.class)
public class DoclabApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoclabApplication.class, args);
    }

}
