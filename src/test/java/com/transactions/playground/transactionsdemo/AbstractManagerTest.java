package com.transactions.playground.transactionsdemo;

import com.transactions.playground.transactionsdemo.model.Event;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractManagerTest {

    @Autowired
    protected PlatformTransactionManager platformTransactionManager;

    @Autowired
    protected DataSource dataSource;

    @After
    public void cleanAfterTest() throws SQLException {
        cleanResources();
        cleanDatabaseData();
    }

    private void cleanResources() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        for (Object key : new HashSet(TransactionSynchronizationManager.getResourceMap().keySet())) {
            TransactionSynchronizationManager.unbindResourceIfPossible(key);
        }
    }

    private void cleanDatabaseData() throws SQLException {
        dataSource.getConnection().prepareStatement("delete from event").executeUpdate();
    }

    public void assertEvents(List<Event> events, String ... descriptions) {
        assertThat(events).extracting("description").containsExactly(descriptions);
    }
}
