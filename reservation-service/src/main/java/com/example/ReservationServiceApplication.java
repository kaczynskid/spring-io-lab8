package com.example;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

@Slf4j
@RestController
@RequestMapping("/reservations")
@EnableConfigurationProperties(ReservationsConfig.class)
class ReservationController {

	final ReservationsConfig config;

	final Map<String, Reservation> reservations;

	public ReservationController(ReservationsConfig config) {
		this.config = config;
		this.reservations = Arrays
				.stream(config.getNames().split(","))
				.collect(toMap(identity(), Reservation::new));
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	public List<Reservation> list() {
		return reservations.values().stream().collect(toList());
	}

	@GetMapping(path = "/{name}", produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<Reservation> findOne(@PathVariable("name") String name) {
		return Optional.ofNullable(reservations.get(name))
				.map(ResponseEntity::ok)
				.orElse(notFound().build());
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<?> create(@RequestBody Reservation reservation) {
		log.info("Creating: {}", reservation);
		if (reservations.containsKey(reservation.name)) {
			return status(CONFLICT).build();
		}
		reservations.put(reservation.name, reservation);
		return created(selfUri(reservation)).build();
	}

	private static URI selfUri(Reservation reservation) {
		return linkTo(methodOn(ReservationController.class).findOne(reservation.name))
				.toUri();
	}

	@DeleteMapping(path = "/{name}")
	@ResponseStatus(NO_CONTENT)
	public void delete(@PathVariable("name") String name) {
		reservations.remove(name);
	}
}

@NoArgsConstructor
@AllArgsConstructor
@Data
class Reservation {

	String name;

}

@Data
@ConfigurationProperties("reservation")
class ReservationsConfig {

	String names;
}
