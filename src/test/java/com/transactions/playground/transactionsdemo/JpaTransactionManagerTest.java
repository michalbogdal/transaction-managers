package com.transactions.playground.transactionsdemo;

import com.transactions.playground.transactionsdemo.model.Event;
import com.transactions.playground.transactionsdemo.repo.JDBCEventRepository;
import com.transactions.playground.transactionsdemo.repo.JpaEventRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JpaTransactionManager easily handles both JDBC and JPA. All changes are in the same transaction.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class JpaTransactionManagerTest {

    @Configuration
    static class TestBeanConfiguration extends TestConfig {

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setDataSource(dataSource());
            transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
            transactionManager.setJpaProperties(hibernateProperties());
            return transactionManager;
        }
    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private JDBCEventRepository jdbcEventRepository;

    @Autowired
    private JpaEventRepository jpaEventRepository;


    @Test
    public void shouldSaveEvent() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventTwo = new Event(2L, "descB");
        Event eventThree = new Event(3L, "descC");

        jdbcEventRepository.save(eventTwo);
        jpaEventRepository.save(eventThree);

        platformTransactionManager.commit(transaction);

        List<Event> jdbcEvents = jdbcEventRepository.findAll();
        assertThat(jdbcEvents).hasSize(2);

        List<Event> jpaEvents = jpaEventRepository.findAll();
        assertThat(jpaEvents).hasSize(2);
    }

    @Test
    public void shouldRollbackAllChanges() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventTwo = new Event(2L, "descB");
        Event eventThree = new Event(3L, "descC");

        jdbcEventRepository.save(eventTwo);
        jpaEventRepository.save(eventThree);

        List<Event> jdbcEvents = jdbcEventRepository.findAll();
        assertThat(jdbcEvents).hasSize(2);

        List<Event> jpaEvents = jpaEventRepository.findAll();
        assertThat(jpaEvents).hasSize(2);

        platformTransactionManager.rollback(transaction);

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).isEmpty();
    }
}
