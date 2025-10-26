package com.pultyn.spring_oauth;

import org.springframework.boot.SpringApplication;

public class TestSpringOauthApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringOauthApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
