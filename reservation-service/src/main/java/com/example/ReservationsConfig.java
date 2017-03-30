package com.example;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("reservation")
public class ReservationsConfig {

	String names;
}
