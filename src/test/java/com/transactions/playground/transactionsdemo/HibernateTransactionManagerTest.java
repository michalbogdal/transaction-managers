package com.transactions.playground.transactionsdemo;

import com.transactions.playground.transactionsdemo.model.Event;
import com.transactions.playground.transactionsdemo.repo.JDBCEventRepository;
import com.transactions.playground.transactionsdemo.repo.JpaEventRepository;
import com.transactions.playground.transactionsdemo.repo.SessionEventRepository;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.test.context.ContextConfiguration;
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
 * HibernateTransactionManager easily handles both JDBC and SessionFactory. All changes using plain SQL or hibernate are registered under the same transaction.
 *
 * However if start handling transaction on lower level e.g. using sessionFactory or connection, changes done using sql or hibernate will not be under same transaction,
 * this is because JDBC in this case is not aware of hibernate and vice versa - two transactions will be registered in TransactionSynchronizationManager
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class HibernateTransactionManagerTest extends AbstractManagerTest {

    @Configuration
    @EnableTransactionManagement
    static class TestBeanConfiguration extends TestConfig{


        @Bean
        public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
            HibernateTransactionManager transactionManager = new HibernateTransactionManager();
            transactionManager.setSessionFactory(sessionFactory);
            return transactionManager;
        }
    }

    @Autowired
    private JDBCEventRepository jdbcEventRepository;

    @Autowired
    private SessionEventRepository sessionEventRepository;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void shouldRollbackOnlyJDBCTransaction() throws SQLException {
        TransactionSynchronizationManager.initSynchronization();
        DataSource dataSource = SessionFactoryUtils.getDataSource(sessionFactory);
        Connection connection = DataSourceUtils.getConnection(dataSource);
        connection.setAutoCommit(false);

        Event eventOne = new Event(10L, "descAAA");
        Event eventTwo = new Event(22L, "descBFFF");

        sessionEventRepository.save(eventOne);
        jdbcEventRepository.save(eventTwo);

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).hasSize(2);

        connection.rollback();

        //Hibernate transaction is not rollback so still visible (eventOne)
        //session factory is not aware of jdbc and vice versa

        List<Event> jdbcEvents = jdbcEventRepository.findAll();
        assertThat(jdbcEvents).hasSize(1);
        assertThat(jdbcEvents).extracting("description").containsExactly(eventOne.getDescription());

        List<Event> hibernateEvent = sessionEventRepository.findAll();
        assertThat(hibernateEvent).hasSize(1);
        assertThat(hibernateEvent).extracting("description").containsExactly(eventOne.getDescription());
    }

    @Test
    public void shouldRollbackOnlyHibernateTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        Transaction transaction = sessionFactory.getCurrentSession().beginTransaction();

        Event eventOne = new Event(10L, "descAAA");
        Event eventTwo = new Event(22L, "descBFFF");

        sessionEventRepository.save(eventOne);
        jdbcEventRepository.save(eventTwo);

        //JDBC see their own object because it created separate transaction kept in TransactionSynchronizationManager
        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).hasSize(1);

        List<Event> events2 = sessionEventRepository.findAll();
        assertThat(events2).hasSize(2);

        transaction.rollback();

        //JDBC transaction is not rollback so still visible (eventTwo)
        //session factory is not aware of jdbc and vice versa

        List<Event> eventFromJdbcTransaction = jdbcEventRepository.findAll();
        assertThat(eventFromJdbcTransaction).hasSize(1);
        assertThat(eventFromJdbcTransaction).extracting("description").containsExactly(eventTwo.getDescription());

        List<Event> eventsAfterRollback = sessionEventRepository.findAll();
        assertThat(eventsAfterRollback).hasSize(1);
        assertThat(eventsAfterRollback).extracting("description").containsExactly(eventTwo.getDescription());
    }

    @Test
    public void shouldSaveEvent() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventOne = new Event(1L, "descA");
        Event eventTwo = new Event(2L, "descB");

        sessionEventRepository.save(eventOne);
        jdbcEventRepository.save(eventTwo);

        platformTransactionManager.commit(transaction);

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).hasSize(2);
    }

    @Test
    public void shouldRollbackAllChanges() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventTwo = new Event(2L, "descB");
        Event eventThree = new Event(3L, "descC");

        jdbcEventRepository.save(eventTwo);
        sessionEventRepository.save(eventThree);

        platformTransactionManager.rollback(transaction);

        List<Event> events = jdbcEventRepository.findAll();
        assertThat(events).isEmpty();
    }
}
