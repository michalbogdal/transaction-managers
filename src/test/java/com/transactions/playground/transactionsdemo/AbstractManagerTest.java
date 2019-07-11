package com.transactions.playground.transactionsdemo;

import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;

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
        TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
        dataSource.getConnection().prepareStatement("delete from event").executeUpdate();
        platformTransactionManager.commit(transaction);
    }
}
