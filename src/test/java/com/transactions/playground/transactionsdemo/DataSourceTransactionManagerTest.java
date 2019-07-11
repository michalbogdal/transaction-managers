package com.transactions.playground.transactionsdemo;

import com.transactions.playground.transactionsdemo.model.Event;
import com.transactions.playground.transactionsdemo.repo.JDBCEventRepository;
import com.transactions.playground.transactionsdemo.repo.JpaEventRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSourceTransactionManager can handle only one datasource
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class DataSourceTransactionManagerTest extends AbstractManagerTest {

    @Configuration
    static class TestBeanConfiguration extends TestConfig {

        @Bean
        public PlatformTransactionManager transactionManager() {
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
            transactionManager.setDataSource(dataSource());
            return transactionManager;
        }
    }

    @Autowired
    private JDBCEventRepository jdbcEventRepository;

    @Autowired
    private JpaEventRepository jpaEventRepository;

    @Test
    public void shouldReturnTwoDifferentConnectionsIfNoTransaction() {
        Connection connection1 = DataSourceUtils.getConnection(dataSource);
        Connection connection2 = DataSourceUtils.getConnection(dataSource);

        assertThat(connection1).isNotEqualTo(connection2);
    }

    @Test
    public void shouldReturnSameConnectionsIfTransaction() {
        TransactionSynchronizationManager.initSynchronization();

        Connection connection1 = DataSourceUtils.getConnection(dataSource);
        Connection connection2 = DataSourceUtils.getConnection(dataSource);

        assertThat(connection1).isEqualTo(connection2);
    }

    @Test
    public void shouldSaveEventUsingLowLevelTransactions() throws SQLException {
        TransactionSynchronizationManager.initSynchronization();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        connection.setAutoCommit(false);

        Event eventOne = new Event(10L, "descABC");
        Event eventTwo = new Event(21L, "descBDE");
        jdbcEventRepository.save(eventOne);
        jdbcEventRepository.save(eventTwo);

        connection.commit();

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).hasSize(2);
    }

    @Test
    public void shouldRollbackChangesUsingLowLevelTransactions() throws SQLException {
        TransactionSynchronizationManager.initSynchronization();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        connection.setAutoCommit(false);

        Event eventOne = new Event(10L, "descABC");
        Event eventTwo = new Event(21L, "descBDE");
        jdbcEventRepository.save(eventOne);
        jdbcEventRepository.save(eventTwo);

        connection.rollback();

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).isEmpty();
    }

    @Test
    public void shouldSaveEvent() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventTwo = new Event(2L, "descB");
        jdbcEventRepository.save(eventTwo);

        platformTransactionManager.commit(transaction);

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).hasSize(1);
    }

    @Test
    public void shouldRollbackAllChanges() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventTwo = new Event(2L, "descB");

        jdbcEventRepository.save(eventTwo);

        platformTransactionManager.rollback(transaction);

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).isEmpty();
    }

    @Test
    public void shouldCommitOnlyJDBCTransaction() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
        Event eventOne = new Event(1L, "descA");
        Event eventTwo = new Event(2L, "descB");
        jpaEventRepository.save(eventTwo);
        jdbcEventRepository.save(eventOne);
        platformTransactionManager.commit(transaction);

        List<Event> jdbcEvents = jdbcEventRepository.findAll();
        assertThat(jdbcEvents).hasSize(1);
        assertThat(jdbcEvents).extracting("description").containsExactly(eventOne.getDescription());

        /*
         * after commit JPA return entry persisted by JDBC repository
         */
        List<Event> jpaEvents = jpaEventRepository.findAll();
        assertThat(jpaEvents).hasSize(1);
        assertThat(jpaEvents).extracting("description").containsExactly(eventOne.getDescription());
    }

    @Test
    public void shouldRollbackJDBCTransaction() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
        Event eventOne = new Event(1L, "descA");
        Event eventTwo = new Event(2L, "descB");
        jpaEventRepository.save(eventTwo);
        jdbcEventRepository.save(eventOne);
        platformTransactionManager.rollback(transaction);

        List<Event> jdbcEvents = jdbcEventRepository.findAll();
        assertThat(jdbcEvents).isEmpty();

        /*
         * after commit JPA return entry persisted by JDBC repository
         */
        List<Event> jpaEvents = jpaEventRepository.findAll();
        assertThat(jpaEvents).isEmpty();
    }
}
