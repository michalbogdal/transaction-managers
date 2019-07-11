#### **_Transactions... my friends._**

**Transaction managers** are responsible as name suggest for managing transactions. They decides if a new transaction should be created, if they should join to already created one and if it should be committed or rollback.

Spring provides one common abstraction for Transaction Managers:  **PlatformTransactionManager**, thanks to it the source code doesn't need to be aware of particular manager, so there is no need for any changes when transaction manager is replaced. 

Different implementation deals with different abstractions: session factory  (hibernate), entity manager (jpa), datasource (jdbc) etc. 
Sometimes they can handle couple of resources under same transaction like JDBC and ORM, this is only possible if they **share the same connection** to database.

If we have different resources (database and JMS), or many databases (oracle, mysql etc) only one option to have atomic transaction is to use JtaTransactionManager and _two phase commit_


Declarative approach by using **@Transactional** annotation, makes the transaction handling stuff fully transparent.

Transaction Inteceptor which work under the hood for us, does something like this: 
```java
@Autowired
private PlatformTransactionManager platformTransactionManager;
...


TransactionStatus transaction = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
//logic
platformTransactionManager.commit(transaction);

//or
platformTransactionManager.rollback(transaction);

```
and PlatformTransactionManager could be any transaction manager (jta, jpa, hibernate, datasource, rabbit etc)


transaction can be also handled on lower levels:
```java
TransactionSynchronizationManager.initSynchronization();
Connection connection = DataSourceUtils.getConnection(dataSource);
connection.setAutoCommit(false);

connection.commit();
...
connection.rollback();
```

```java
TransactionSynchronizationManager.initSynchronization();
Transaction transaction = sessionFactory.getCurrentSession().beginTransaction();

transaction.commit();
...
transaction.rollback();
```

From transactional point of view **TransactionSynchronizationManager** is quite important. 
It is kind of the central storage for resources and connections.
It is also quite good as starting point for **debugging**.

Test cases written here presents different configuration of transaction managers, mixed resources etc.


Transaction managers brief summary:

* **JtaTransactionManager** (JTA)  - enterprise usage (in most cases delegated to application server)
    * many DataSources -> Connections

* **DataSourceTransactionManager** (JDBC) - e.g. only plain SQL
    * DataSource -> Connection

* **JpaTransactionManager** (JPA) - e.g. both plain SQL and hibernate
    * EntityManager -> DataSource -> connection
    * DataSource -> connection
    
* **HibernateTransactionManager** (Hibernate)  - e.g. both plain SQL and hibernate
    * SessionFactory -> Session -> Connection
    * DataSource -> Connection
    
* **ChainedTransactionManager** (we can have something like _"best effort 1pc"_)
    * transactionManagers    
    
* **RabbitTransactionManager**  (Rabbit message broker)  
    * ConnectionFactory