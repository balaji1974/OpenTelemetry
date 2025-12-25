package com.java.bala.springboot.otel_metrics_exporter.controller;

import java.util.Random;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping ("/random")
public class RandomNumberGenerator {

	@GetMapping ("/{id}")
	public Integer findbyId(@PathVariable Integer id) {
		
		// nextInt(int bound) generates a number from 0 up to (but not including) the bound
        return new Random().nextInt(id);
	}
	
}
