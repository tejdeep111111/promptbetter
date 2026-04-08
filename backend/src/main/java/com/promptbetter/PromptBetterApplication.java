package com.promptbetter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PromptBetterApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromptBetterApplication.class, args);
	}

}
