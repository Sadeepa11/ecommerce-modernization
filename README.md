# TechMart Online — E-Commerce Platform Modernization Project

Welcome to the **TechMart Online E-Commerce Platform Modernization** repository. This project is structured as a Java Enterprise Edition (Jakarta EE) Multi-Module Enterprise Archive (EAR) application, designed to run on the **Payara 6 Application Server** with **MySQL 8.0**.

This guide outlines the system architecture and provides step-by-step instructions to compile, configure, and run this application on another PC.

---

## 🏗️ Multi-Module Enterprise Architecture
The application is partitioned into three dedicated modules to achieve strict separation of concerns and high scalability:
1. **`techmart-ejb`**: Business logic module containing transactional Enterprise JavaBeans (EJBs), JPA entities (Hibernate), JMS message listeners, and configuration descriptors.
2. **`techmart-war`**: Presentation and controller module hosting servlets, API endpoints, and the HTML/CSS web UI.
3. **`techmart-ear`**: Packaging module that consolidates the EJB and WAR modules along with the database connector libraries into a single enterprise archive (`.ear`) for server deployment.

---

## ⚙️ Prerequisites (Required Tools)
To run this project on another PC, ensure the following software is installed and configured in the system environment:
*   **Java JDK (Version 11 or 17)**: Mapped to the `JAVA_HOME` environment variable and added to the system `PATH`.
*   **Apache Maven (Version 3.6+)**: Configured in the system `PATH` to manage compilation and packaging.
*   **MySQL Server (Version 8.0+)**: Running locally on port `3306`.
*   **Payara Server 6**: Installed on the target machine (e.g., `C:\payara6` or `D:\payara6`).

---

## 🚀 Execution Steps on a New PC

### Step 1: Set Up the Database
1. Open your MySQL client (CLI or Workbench) and run the following command to create the database:
   ```sql
   CREATE DATABASE IF NOT EXISTS techmart_db;
   ```
2. **Credentials Configuration**:
   The application is configured to connect to MySQL using:
   *   **Username**: `root`
   *   **Password**: `Sata.Pata.123` (configured in `PlatformMetricsRegistry.java`)
   
   *Note: If your local MySQL password differs, you can update the `@DataSourceDefinition` annotation password field in [PlatformMetricsRegistry.java](file:///C:/Users/97150/Desktop/ecommerce-modernization/techmart-ejb/src/main/java/com/techmart/ejb/PlatformMetricsRegistry.java) or create a MySQL user matching these credentials.*
   
3. **Schema Generation**:
   You do not need to manually import any database tables! The application is configured with JPA Auto DDL (`drop-and-create` / `drop-and-create-tables`) inside [persistence.xml](file:///C:/Users/97150/Desktop/ecommerce-modernization/techmart-ejb/src/main/resources/META-INF/persistence.xml). The database tables will be auto-generated in MySQL the moment the application is deployed.
   *Optional: If you want to view or manually import the database schema, it is located in [techmart_optimized_schema.sql](file:///C:/Users/97150/Desktop/ecommerce-modernization/techmart_optimized_schema.sql).*

---

### Step 2: Build the Application (Generate EAR)
1. Open a terminal (Command Prompt, PowerShell, or Bash) in the project's root folder (`ecommerce-modernization`).
2. Run the following Maven command to compile and build the package:
   ```bash
   mvn clean package
   ```
3. Once the build finishes successfully, the packaged Enterprise Archive (`.ear`) file will be created at:
   ```
   techmart-ear/target/ecommerce-modernization.ear
   ```

---

### Step 3: Start Payara Server
1. Open a terminal and navigate to the `bin` folder of your local Payara installation:
   *   **Windows**: `cd C:\payara6\bin`
   *   **Linux/macOS**: `cd /path/to/payara6/bin`
2. Run the command to start the default server domain (`domain1`):
   *   **Windows**:
       ```powershell
       .\asadmin.bat start-domain domain1
       ```
   *   **Linux/macOS**:
       ```bash
       ./asadmin start-domain domain1
       ```

---

### Step 4: Deploy the Application
There are two easy methods to deploy the application on your server:

#### Method A: Auto-Deploy (Recommended & Quickest)
1. Copy the built enterprise archive: `techmart-ear/target/ecommerce-modernization.ear`
2. Navigate to your Payara domain autodeploy directory:
   *   **Path**: `<payara-installation-folder>/glassfish/domains/domain1/autodeploy/`
3. Paste the `ecommerce-modernization.ear` file inside. The server will detect it and deploy the application automatically.

#### Method B: Admin Console (GUI Interface)
1. Open your browser and access the Payara Administration Console: **[http://localhost:4848/](http://localhost:4848/)**
2. In the left panel, click on **Applications**.
3. Click the **Deploy** button.
4. Select **Packaged File to be Uploaded to the Server** and choose the `ecommerce-modernization.ear` file.
5. Leave default settings and click **OK** at the top right to deploy.

*Note: Since resources (MySQL JDBC DataSource and JMS Queues) are declared programmatically in the code via `@DataSourceDefinition` and `@JMSDestinationDefinition`, you do NOT need to create resources manually inside the Payara Admin Console.*

---

### Step 5: Access the Web Application
Once the application is successfully deployed:
1. Open your web browser and navigate to:
   👉 **[http://localhost:8080/ecommerce-modernization/](http://localhost:8080/ecommerce-modernization/)**
2. **Seed the Database**: On your first run, you need to populate sample products in the store database. Execute the seed API endpoint:
   👉 **[http://localhost:8080/ecommerce-modernization/api/seed](http://localhost:8080/ecommerce-modernization/api/seed)**
3. Refresh the main shop UI, and the items will load from the database.

---

### 🛑 Stopping the Payara Server
If you need to stop the server domain, run the following command from the Payara `bin` folder:
*   **Windows**: `.\asadmin.bat stop-domain domain1`
*   **Linux/macOS**: `./asadmin stop-domain domain1`

---

## 🇱🇰 ව්‍යාපෘතිය ක්‍රියාත්මක කිරීමේ සිංහල මාර්ගෝපදේශය (Quick Guide)

1. **ඩේටාබේස් සකස් කිරීම**:
   ඔබගේ MySQL Server එකට සම්බන්ධ වී `CREATE DATABASE techmart_db;` ලෙස ඩේටාබේස් එකක් සාදන්න. (ප්‍රොජෙක්ට් එකේ SQL user: `root`, password: `Sata.Pata.123` විය යුතුය).
   
2. **ප්‍රොජෙක්ට් එක Build කිරීම**:
   Terminal එකක් හරහා ප්‍රොජෙක්ට් එකෙහි root directory එකට ගොස් `mvn clean package` command එක ක්‍රියාත්මක කරන්න. (`techmart-ear/target/ecommerce-modernization.ear` ගොනුව සෑදේ).

3. **Payara Server එක ආරම්භ කිරීම**:
   Payara bin folder එකට ගොස් `.\asadmin.bat start-domain domain1` මඟින් සර්වර් එක ආරම්භ කරන්න.

4. **Deploy කිරීම**:
   Build කරන ලද `ecommerce-modernization.ear` ගොනුව copy කර `<payara6-folder>/glassfish/domains/domain1/autodeploy` ෆෝල්ඩරය තුළට paste කරන්න.

5. **භාවිතය**:
   බ්‍රවුසර් එකෙන් **[http://localhost:8080/ecommerce-modernization/](http://localhost:8080/ecommerce-modernization/)** වෙත පිවිසෙන්න. මුලින්ම **[http://localhost:8080/ecommerce-modernization/api/seed](http://localhost:8080/ecommerce-modernization/api/seed)** ලිපිනය ධාවනය කර ඩේටාබේස් එකට sample products ඇතුලත් කරගන්න.
