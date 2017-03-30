package com.example;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ReservationsPersistenceTest {

    @Autowired TestEntityManager entityManager;
    @Autowired ReservationRepository reservations;

    @Test
    public void should_persist_reservations() throws Exception {
    }

    @Test
    public void should_find_by_name() throws Exception {
    }
}
