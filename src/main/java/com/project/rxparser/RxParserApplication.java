package com.project.rxparser;

import com.project.rxparser.config.RxBatchConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RxBatchConfiguration.class)
public class RxParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(RxParserApplication.class, args);
	}

}
