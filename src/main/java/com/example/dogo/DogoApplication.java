package com.example.dogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class DogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DogoApplication.class, args);
		System.out.println("http://localhost:8080");
	}

}
