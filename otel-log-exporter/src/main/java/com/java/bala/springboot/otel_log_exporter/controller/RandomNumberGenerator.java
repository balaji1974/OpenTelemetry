package com.java.bala.springboot.otel_log_exporter.controller;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping ("/random")
public class RandomNumberGenerator {
	
	private final Logger LOG = LoggerFactory.getLogger(RandomNumberGenerator.class);

	@GetMapping ("/{id}")
	public Integer findbyId(@PathVariable Integer id) {
		LOG.info("Input number is : {}", id);
		Integer randomNumber=new Random().nextInt(id);
		LOG.info("Generated random number is : {}", randomNumber);
        return randomNumber;
	}
	
}
