package com.kospot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KoSpotApplication {

	public static void main(String[] args) {
		SpringApplication.run(KoSpotApplication.class, args);
	}

}
