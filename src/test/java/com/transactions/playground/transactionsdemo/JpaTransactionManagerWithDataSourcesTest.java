package com.transactions.playground.transactionsdemo;

import com.transactions.playground.transactionsdemo.model.Event;
import com.transactions.playground.transactionsdemo.repo.JDBCEventRepository;
import com.transactions.playground.transactionsdemo.repo.JpaEventRepository;
import org.h2.Driver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@RunWith(SpringRunner.class)
public class JpaTransactionManagerWithDataSourcesTest extends AbstractManagerTest{

    @Configuration
    static class TestBeanConfiguration extends TestConfig {

        @Bean
        public DataSource dataSource() {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriver(new Driver());
            dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
            return dataSource;
        }

        @Bean
        public DataSource dataSource2() {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriver(new Driver());
            dataSource.setUrl("jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
            return dataSource;
        }

        @Bean
        public JdbcTemplate jdbcTemplate() {
            JdbcTemplate template = new JdbcTemplate(dataSource());
            return template;
        }

        @Bean
        public JdbcTemplate jdbcTemplate2() {
            JdbcTemplate template2 = new JdbcTemplate(dataSource2());
            return template2;
        }

        /**
         * JpaTransactionManager configured with 2 different dataSources
         *  - entityManagerFactory -> dataSource
         *  - dataSource2
         *  however JpaTranasctionManager.afterPropertiesSet will overwrite dataSource taken from entityManagerFactory
         *
         * jdbcTemplate (dataSource) is together with entityManager = one transaction
         * jdbcTemplate2 (dataSource2) is another transaction
         */
        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            /*
            setDataSource has no any impact since no matter if we put dataSource or not,
            it will be overwritten by dataSource provided by entityManager (hibernate implementation in this case)
             */
            transactionManager.setDataSource(dataSource2());

            transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
            transactionManager.setJpaProperties(hibernateProperties());
            return transactionManager;
        }

    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private JpaEventRepository jpaEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate2;

    private JDBCEventRepository jdbcEventRepository1;

    private JDBCEventRepository jdbcEventRepository2;

    @Before
    public void setUp(){
        jdbcEventRepository1 =  new JDBCEventRepository(jdbcTemplate);
        jdbcEventRepository2 =  new JDBCEventRepository(jdbcTemplate2);

        createDefaultDB(jdbcTemplate.getDataSource());
        createDefaultDB(jdbcTemplate2.getDataSource());
    }

    @After
    public void cleaning() throws SQLException {
        jdbcTemplate.getDataSource().getConnection().prepareStatement("delete from event").executeUpdate();
        jdbcTemplate2.getDataSource().getConnection().prepareStatement("delete from event").executeUpdate();
    }

    public void createDefaultDB(DataSource dataSource) {
        Resource resource = new ClassPathResource("import.sql");
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(resource);
        databasePopulator.execute(dataSource);
    }

    @Test
    public void shouldSaveEventInTwoTransactions() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventOne = new Event(233L, "descAAAA");
        Event eventTwo = new Event(211L, "descBBBBB");
        Event eventThree = new Event(344L, "descCCCC");

        jdbcEventRepository1.save(eventOne);
        jdbcEventRepository2.save(eventTwo);
        jpaEventRepository.save(eventThree);

        platformTransactionManager.commit(transaction);

        List<Event> jdbcEvents1 = jdbcEventRepository1.findAll();
        assertThat(jdbcEvents1).hasSize(2);
        assertEvents(jdbcEvents1, "descAAAA", "descCCCC");

        List<Event> jdbcEvents2 = jdbcEventRepository2.findAll();
        assertThat(jdbcEvents2).hasSize(1);
        assertEvents(jdbcEvents2, "descBBBBB");

        List<Event> jpaEvents = jpaEventRepository.findAll();
        assertThat(jpaEvents).hasSize(2);
        assertEvents(jpaEvents, "descAAAA", "descCCCC");
    }

    @Test
    public void shouldRollbackChangesOfOneTransaction() {
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        Event eventOne = new Event(111L, "descAA");
        Event eventTwo = new Event(233L, "descBB");
        Event eventThree = new Event(311L, "descCC");

        jdbcEventRepository1.save(eventOne);
        jdbcEventRepository2.save(eventTwo);
        jpaEventRepository.save(eventThree);

        List<Event> jdbcEvents = jdbcEventRepository1.findAll();
        assertThat(jdbcEvents).hasSize(2);
        assertEvents(jdbcEvents, "descAA", "descCC");

        List<Event> jdbcEvents2 = jdbcEventRepository2.findAll();
        assertThat(jdbcEvents2).hasSize(1);
        assertEvents(jdbcEvents2, "descBB");

        List<Event> jpaEvents = jpaEventRepository.findAll();
        assertThat(jpaEvents).hasSize(2);
        assertEvents(jpaEvents, "descAA", "descCC");

        platformTransactionManager.rollback(transaction);

        List<Event> rollbackJdbcEvents = jdbcEventRepository1.findAll();
        assertThat(rollbackJdbcEvents).isEmpty();

        List<Event> rollbackJdbcEvents2 = jdbcEventRepository2.findAll();
        assertThat(rollbackJdbcEvents2).hasSize(1);
        assertEvents(jdbcEvents2, "descBB");

        List<Event> rollbackJpaEvents = jpaEventRepository.findAll();
        assertThat(rollbackJpaEvents).isEmpty();
    }
}
