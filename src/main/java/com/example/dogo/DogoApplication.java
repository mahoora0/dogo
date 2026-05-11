package com.example.dogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DogoApplication.class, args);
	}

}
