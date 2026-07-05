-- =============================================================================
-- TECHMART ONLINE E-COMMERCE DATABASE SCHEMA & OPTIMIZATION CONFIGURATIONS
-- Target Database Engine: MySQL 8.0+ (InnoDB)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- PART 1: RELATIONAL SCHEMA DEFINITIONS (DDL)
-- -----------------------------------------------------------------------------

-- Enable integrity checks
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Table: Products
CREATE TABLE IF NOT EXISTS `products` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `PRICE` double NOT NULL,
  `SKU` varchar(255) NOT NULL,
  `STOCK` int NOT NULL,
  `warehouse_location` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `SKU` (`SKU`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Table: Orders
CREATE TABLE IF NOT EXISTS `orders` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `customer_name` varchar(255) NOT NULL,
  `processing_time_ms` bigint DEFAULT NULL,
  `STATUS` varchar(255) NOT NULL,
  `total_price` double NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Table: Order Items (One-to-Many Relationship to Orders)
CREATE TABLE IF NOT EXISTS `order_items` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `price` double NOT NULL,
  `product_sku` varchar(255) NOT NULL,
  `QUANTITY` int NOT NULL,
  `order_id` bigint NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_order_items_order_id` (`order_id`),
  CONSTRAINT `FK_order_items_order_id` FOREIGN KEY (`order_id`) REFERENCES `orders` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- -----------------------------------------------------------------------------
-- PART 2: RESOURCE & PERFORMANCE OPTIMIZATION CONFIGURATIONS
-- -----------------------------------------------------------------------------

/*
  A. DATABASE CONNECTION POOLING OPTIMIZATIONS (@DataSourceDefinition)
  -----------------------------------------------------------------------------
  The MySQL data source is registered dynamically inside the EJB container:
  - JNDI Name: java:app/jdbc/TechMartDS
  - Driver Class: com.mysql.cj.jdbc.MysqlDataSource
  - Target URL: jdbc:mysql://localhost:3306/techmart_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
  - Credentials: User=root / Password=Sata.Pata.123
  - Release Mode: 'auto' (configured in persistence.xml) - optimizes transactional 
    concurrency by returning connections to the pool immediately after commit.

  B. APPLICATION SERVER THREAD POOL BOUNDARIES (Payara server-config)
  -----------------------------------------------------------------------------
  Located in domain.xml, managing concurrency and avoiding resource exhaustion:
  
  1. http-thread-pool (Handles incoming Web Servlet UI & REST traffic):
     - Min Thread Pool Size: 2 (Default)
     - Max Thread Pool Size: 200 (Default)
     - Max Queue Size: 4096 (Default)
     
  2. thread-pool-1 (Dedicated to IIOP ORB remote EJB client interactions):
     - Min Thread Pool Size: 2 (Default)
     - Max Thread Pool Size: 200
     - Max Queue Size: Unlimited
     
  3. admin-thread-pool (Allocated for Payara administration console):
     - Min Thread Pool Size: 1
     - Max Thread Pool Size: 15
     - Max Queue Size: 256

  C. EJB CONCURRENCY & THREAD-SAFETY OPTIMIZATIONS
  -----------------------------------------------------------------------------
  1. PlatformMetricsRegistry (Startup Singleton):
     - Uses Container-Managed Concurrency (CMC) to protect metrics telemetry.
     - @Lock(LockType.READ) on getter methods permits non-blocking concurrent reads.
     - @Lock(LockType.WRITE) on setter methods enforces atomic updates.
     - @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) prevents
       long-running database transactions from blocking registry telemetry.

  2. NotificationService (Asynchronous Worker):
     - @Asynchronous execution delegates sendNotificationAsync to a container thread pool.
     - Spawns background threads returning Future<Boolean> (AsyncResult) to handle
       simulated 1.5-second SMTP mail server latencies without blocking the HTTP request thread.

  3. OrderProcessorMDB (JMS Queue Listener):
     - @MessageDriven listener consuming from java:app/jms/OrderQueue.
     - Multi-threaded processing handled automatically by Payara's MDB instance pool.
     - Acknowledge Mode: Auto-acknowledge.
*/
