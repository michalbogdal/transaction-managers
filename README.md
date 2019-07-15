#### **_Transactions... my friends._**

**Transaction managers** have similar goals, to bound treads with resources and decide if a new transaction should be created, previous suspended, or maybe to continue already created one. 
Apart from this they make a final decision if transaction should be committed or rollback.

**Spring framework** provides one common abstraction for Transaction Managers:  **PlatformTransactionManager**, thanks to it the source code doesn't need to be aware of particular manager, so there is no need for any changes when transaction manager is replaced. 

Different implementations deals with different abstractions: session factory  (hibernate), entity manager (jpa), datasource (jdbc) etc. 
Sometimes they can handle couple of resources under same transaction like JDBC and ORM, this is only possible if they **share the same connection** to database.

If we have different resources (database and JMS), or many databases (oracle, mysql etc) only one option to have global atomic transaction (XA) is to use JtaTransactionManager and _two phase commit_.
 The question should be if we really need it. It depends on particular system and requirements. Always it is a matter of trade off.
 Good alternative could be "_Best effort 1pc_" (see links), transaction compensation and other patterns of distributed transactions.

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
and there is many PlatformTransactionManager implementation for instance (jta, jpa, hibernate, datasource, weblogic, rabbit etc)


We can also go lower and handle transaction on datasource or sessionFactory level:
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

------------------

One common place used by transaction managers, resources and any logic which is aware of transaction is **TransactionSynchronizationManager**. 
It is kind of the central storage for resources and connections.
It is also quite good as starting point for **debugging**.

For instance:
* if method is annotated with @Transactional, transaction manager begin the transaction (sets some flags, initializes collections, bounds threads with connections) - e.g. in case of HibernateTransactionManager it stores connection for DataSource and Session for SessionFactory, so anyone can reuse them to be in the same transaction.
* Resources like *Template (jdbcTemplate, rabbitTemplate, jmsTemplate etc) synchronize themselves with already started transaction. What I mean by synchronizing, in case of e.g. rabbitTemplate:
    * **rabbitTemplate** checks if connectionFactory is already associated with transaction (registered) to reuse the connection - if so nothing happens since it is already under same transaction
    * if **connectionFactory** is not associated with transaction, **rabbitTemplate** creates new connection for this connectionFactory and register it. New connection, means new transaction so now we have at least two separate transactions. For this case rabbitTemplate also register synchronization logic - in most cases, commit if current transaction is also committed, otherwise rollback (but message broker commit failure will not rollback already committed transaction)
    
    Please see _org.springframework.amqp.rabbit.connection.ConnectionFactoryUtils#bindResourceToTransaction_ and _RabbitResourceSynchronization_.
* any code can also synchronize some logic with transaction (e.g. to postpone logic until transaction is committed) or force rollback

------------------



_Brief **summary** for transaction managers:_

* **JtaTransactionManager** (JTA)  - enterprise usage (in most cases delegated to application server)
    * many resources (supporting XA)

* **DataSourceTransactionManager** (JDBC) - e.g. only plain SQL
    * DataSource -> Connection

* **JpaTransactionManager** (JPA) - e.g. both plain SQL and hibernate
    * EntityManagerFactory -> DataSource -> connection
    * DataSource -> connection
    
* **HibernateTransactionManager** (Hibernate)  - e.g. both plain SQL and hibernate
    * SessionFactory -> Session -> Connection
    * DataSource -> Connection
    
* **ChainedTransactionManager** (we can have something like _"best effort 1pc"_)
    * transactionManagers    
    
* **RabbitTransactionManager**  (Rabbit message broker)  
    * ConnectionFactory
    
* others ...    
    
    
------------------

###### **Q & A**

This is kind of summary but presented in different way - as a questions and answers.

1) If I use jdbc, which transaction manager could I use
    * all transaction managers which support dataSource would work: DataSourceTransactionManager, HibernateTransactionManager, JpaTransactionManager etc however it make sense to use DataSourceTransactionManager (YAGNI - most tailor made for a given scenario) 
    
2) what would happen if I configure JpaTransactionManager **without adding dataSource or entityManagerFactory** to it?    
    * it would fail, since entityManagerFactory is required

3) what would happen if I configure JpaTransactionManager **only without dataSource** (with entityManagerFactory) 
    * transaction manager would take dataSource from entityManagerFactory   
    
4) what would happen if I configure JpaTransactionManager **with different dataSource** (different than used by entityManager)
    * any dataSource configured will be overwritten by dataSource provided by entityManagerFactory (e.g. by hibernate implementation). Two dataSources will be in two separate transactions, but the second one will not be committed after the first one (Please see _org.springframework.jdbc.datasource.DataSourceUtils.doGetConnection_ and _ConnectionSynchronization_) 
    
5) what would happen if I create two instances of dataSource pointing to the same database?
    * they will be anyway in two transactions (if dataSource.equals(dataSource2) == false)     
------------------   
   

##### Please see the source code and test cases which presents different configuration of transaction managers with mixed resources etc.

    
------------------

Very good info about transactions:

https://www.javaworld.com/article/2077963/distributed-transactions-in-spring--with-and-without-xa.html

https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/transaction.html