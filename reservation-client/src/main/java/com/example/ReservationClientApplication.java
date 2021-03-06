package com.example;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCircuitBreaker
public class ReservationClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}

	@Bean
	@ConditionalOnProperty(name = "spring.cloud.discovery.enabled", havingValue = "true", matchIfMissing = true)
	public ApplicationRunner discoveryClientDemo(DiscoveryClient discovery) {
		return args -> {
			try {
				log.info("------------------------------");
				log.info("DiscoveryClient Example");

				discovery.getInstances("reservationservice").forEach(instance -> {
					log.info("Reservation service: ");
					log.info("  ID: {}", instance.getServiceId());
					log.info("  URI: {}", instance.getUri());
					log.info("  Meta: {}", instance.getMetadata());
				});

				log.info("------------------------------");
			} catch (Exception e) {
				log.error("DiscoveryClient Example Error!", e);
			}
		};
	}

	@Bean @LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}

@FeignClient("verifierservice")
interface VerifierClient {

	@PostMapping(path = "/check", consumes = APPLICATION_JSON_VALUE)
	VerifierResponse check(@RequestBody ReservationRequest request);
}

@NoArgsConstructor
@AllArgsConstructor
@Data
class VerifierResponse {
	boolean eligible;
}

@NoArgsConstructor
@AllArgsConstructor
@Data
class ReservationRequest {

	String name;
	int age;

}

@FeignClient(name = "reservationservice/reservations", fallback = ReservationsFallback.class)
interface ReservationsClient {

	@GetMapping
	Resources<Reservation> listReservations();
}

@Component
class ReservationsFallback implements ReservationsClient {

	@Override
	public Resources<Reservation> listReservations() {
		return new Resources<>(Stream.of("This", "is", "fallback")
				.map(Reservation::new).collect(toList()));
	}
}

@Slf4j
@RestController
@RequestMapping("/client")
class ReservationsController {

	private final RestTemplate rest;
	private final ReservationsClient client;
	private final VerifierClient verifier;

	public ReservationsController(RestTemplate rest, ReservationsClient client,
								  VerifierClient verifier) {
		this.rest = rest;
		this.client = client;
		this.verifier = verifier;
	}

	@GetMapping("/names")
	public List<String> names() {
		log.info("Calling names...");
		ParameterizedTypeReference<Resources<Reservation>> responseType =
				new ParameterizedTypeReference<Resources<Reservation>>() {};
		ResponseEntity<Resources<Reservation>> exchange =
				rest.exchange("http://reservationservice/reservations", HttpMethod.GET, null, responseType);
		return exchange.getBody().getContent().stream()
				.map(Reservation::getName)
				.collect(toList());
	}

	@GetMapping("/feign-names")
	public List<String> feignNames() {
		log.info("Calling feign-names...");
		return client.listReservations().getContent().stream()
				.map(Reservation::getName)
				.collect(toList());
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody ReservationRequest request) {
		VerifierResponse check = verifier.check(request);
		if (check.eligible) {
			return ResponseEntity.status(HttpStatus.CREATED).build();
		} else {
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).build();
		}
	}
}

@NoArgsConstructor
@AllArgsConstructor
@Data
class Reservation {

	String name;

}
