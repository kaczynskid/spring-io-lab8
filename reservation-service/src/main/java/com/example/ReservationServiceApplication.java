package com.example;

import static java.lang.String.*;
import static java.lang.System.*;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.String;

@SpringBootApplication
@EnableConfigurationProperties(ReservationsConfig.class)
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

@Slf4j
@Component
@RepositoryEventHandler
class ReservationEventHandler {

	private final CounterService counter;

	public ReservationEventHandler(CounterService counter) {
		this.counter = counter;
	}

	@HandleAfterCreate
	public void create(Reservation reservation) {
		log.info("Created reservation for {}.", reservation.name);
		counter.increment("count");
		counter.increment("create");
	}

	@HandleAfterSave
	public void save(Reservation reservation) {
		log.info("Updated reservation for {}.", reservation.name);
		counter.increment("save");
	}

	@HandleAfterDelete
	public void delete(Reservation reservation) {
		log.info("Removed reservation for {}.", reservation.name);
		counter.decrement("count");
		counter.increment("delete");
	}
}

@Configuration
class ReservationsExtras {

	@Bean
	public ApplicationRunner init(ReservationsConfig config, ReservationRepository reservations) {
		return args -> Arrays
				.stream(config.getNames().split(","))
				.map(Reservation::new)
				.forEach(reservations::save);
	}

	private final Random rng = new Random();

	@Bean
	public HealthIndicator reservationsHealthIndicator() {
		return () -> (rng.nextBoolean() ? Health.up() : Health.down())
				.withDetail("spring", "boot")
				.build();
	}

	@Bean
	public InfoContributor reservationsInfoContributor() {
		return builder -> builder
				.withDetail("currentTime", currentTimeMillis()).build();
	}
}

@Slf4j
@RestController
@RequestMapping("/custom-reservations")
class ReservationController {

	final ReservationRepository reservations;

	public ReservationController(ReservationRepository reservations) {
		this.reservations = reservations;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	public List<Reservation> list() {
		return reservations.findAll();
	}

	@GetMapping(path = "/{name}", produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<Reservation> findOne(@PathVariable("name") String name) {
		return Optional.ofNullable(reservations.findByName(name))
				.map(ResponseEntity::ok)
				.orElse(notFound().build());
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<?> create(@RequestBody Reservation reservation) {
		log.info("Creating: {}", reservation);
		if (reservations.exists(Example.of(reservation))) {
			return status(CONFLICT).build();
		}
		reservations.save(reservation);
		return created(selfUri(reservation)).build();
	}

	private static URI selfUri(Reservation reservation) {
		return linkTo(methodOn(ReservationController.class).findOne(reservation.name))
				.toUri();
	}

	@DeleteMapping(path = "/{name}")
	@ResponseStatus(NO_CONTENT)
	public void delete(@PathVariable("name") String name) {
		reservations.delete(reservations.findByName(name));
	}
}

@Component
class ReservationResourceProcessor implements ResourceProcessor<Resource<Reservation>> {

	@Value("${info.instanceId}") String instanceId;

	@Override
	public Resource<Reservation> process(Resource<Reservation> resource) {
		Reservation reservation = resource.getContent();
		String url = format("https://www.google.pl/search?tbm=isch&q=%s",
				reservation.getName());
		resource.getContent().setName(resource.getContent().getName() + "-" + instanceId);
		resource.add(new Link(url, "photo"));
		return resource;
	}
}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {

	@RestResource(path = "by-name", rel = "find-by-name")
	Reservation findByName(@Param("name") String name);

	@RestResource(exported = false)
	@Override
	void delete(Long id);
}

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = "name")
})
class Reservation {

	@Id
	@GeneratedValue
	Long id;

	String name;

	Reservation(String name) {
		this.name = name;
	}
}
