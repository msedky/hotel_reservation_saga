package com.travelbooking.hotel;

import org.springframework.boot.SpringApplication;

public class TestHotelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(HotelServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
