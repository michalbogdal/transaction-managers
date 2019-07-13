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
* if method is annotated with @Transactional, transaction manager can store initialized transaction there, suspend the current one etc.
* Resource like jdbcTemplate, rabbitTemplate etc can also initialize transaction by itself or synchronize with already created one.
* any code can also synchronize some logic with transaction (e.g. to postpone logic until transaction is committed) 

------------------



_Brief **summary** for transaction managers:_

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
    
* others ...    
    
    
    
------------------   
   

##### Please see the source code and test cases which presents different configuration of transaction managers with mixed resources etc.

    
------------------

Very good info about transactions:

https://www.javaworld.com/article/2077963/distributed-transactions-in-spring--with-and-without-xa.html

https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/transaction.html