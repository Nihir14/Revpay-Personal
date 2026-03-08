package com.revpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RevpayApplication {

	public static void main(String[] args) {
		SpringApplication.run(RevpayApplication.class, args);
	}


}
