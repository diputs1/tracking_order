package com.example.tracking_order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TrackingOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrackingOrderApplication.class, args);
	}

}
