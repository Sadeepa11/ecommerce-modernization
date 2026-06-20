# Project Execution Guide (Payara Server) / ප්‍රොජෙක්ට් එක රන් කරන ආකාරය

මෙම මාර්ගෝපදේශය මඟින් **TechMart Online E-Commerce Modernization** ව්‍යාපෘතිය (Multi-Module EAR Layout) ඔබගේ **Payara 6 Server** එකෙහි සාර්ථකව ක්‍රියාත්මක කරගන්නා ආකාරය පියවරෙන් පියවර පැහැදිලි කරයි.

---

## 🏗️ ව්‍යාපෘති ව්‍යුහය (Multi-Module Layout)
ව්‍යාපෘතිය පහත ආකාරයට මොඩියුල 3ක් සහිත **Enterprise Archive (EAR)** ව්‍යුහයකට සකසා ඇත:
1. **`techmart-ejb`**: Business Logic (EJBs, Database Models, JMS listeners, and [persistence.xml](file:///D:/BCD1/techmart-ejb/src/main/resources/META-INF/persistence.xml)) අඩංගු වේ.
2. **`techmart-war`**: Web Controller ([PlatformControllerServlet.java](file:///D:/BCD1/techmart-war/src/main/java/com/techmart/controller/PlatformControllerServlet.java)), HTML පිටුව ([index.html](file:///D:/BCD1/techmart-war/src/main/webapp/index.html)), CSS, සහ [web.xml](file:///D:/BCD1/techmart-war/src/main/webapp/WEB-INF/web.xml) අඩංගು වේ.
3. **`techmart-ear`**: EJB සහ WAR මොඩියුල එකට එකතු කර deploy කිරීමට භාවිත කරන ප්‍රධාන Enterprise Archive එකයි.

---

## 🛠️ පූර්ව අවශ්‍යතා (Prerequisites)
*   **Java JDK**: version 11/17 (ස්ථාපිතයි)
*   **Apache Maven**: (ස්ථාපිතයි)
*   **MySQL Server**: සක්‍රීයව ක්‍රියාත්මක විය යුතුය (Running)
*   **Payara Server**: `D:\payara6` ස්ථානයේ ස්ථාපනය කර ඇත.

---

## ⚙️ අපි සිදුකල වෙනස්කම් (Configurations Automatically Applied)
1. **JMS Queue Namespace අනුකූලතාව**: [OrderProcessingProducer.java](file:///D:/BCD1/techmart-ejb/src/main/java/com/techmart/jms/OrderProcessingProducer.java) හි interface name එක `jakarta.jms.Queue` ලෙස වෙනස් කර ඇත.
2. **EJB JNDI Lookup යාවත්කාලීන කිරීම**: EJB සහ WAR මොඩියුල වෙන් කිරීම නිසා [PlatformControllerServlet.java](file:///D:/BCD1/techmart-war/src/main/java/com/techmart/controller/PlatformControllerServlet.java) හි ShoppingCart Session lookup එක `java:app/techmart-ejb/ShoppingCartSession` ලෙස වෙනස් කරන ලදී.
3. **EAR Deployment සකස් කිරීම**: [pom.xml](file:///D:/BCD1/techmart-ear/pom.xml) හරහා EJB, WAR සහ MySQL Driver (Connector) එකම EAR එකක් ලෙස package වන පරිදි සකසන ලදී.

---

## 🚀 ක්‍රියාත්මක කරන පියවර (Steps to Run)

### 1 පියවර: ව්‍යාපෘතිය Build කරගැනීම (Build the Project)
පළමුව terminal එකක් හරහා ව්‍යාපෘතියේ root directory එකට (`D:\BCD1`) ගොස් පහත Maven command එක ක්‍රියාත්මක කරන්න:
```bash
mvn clean package
```
*(මෙය සාර්ථකව අවසන් වූ පසු `D:\BCD1\techmart-ear\target\ecommerce-modernization.ear` නමින් Enterprise Archive (.ear) ගොනුවක් සෑදෙනු ඇත)*

---

### 2 පියවර: Payara Server එක ආරම්භ කිරීම (Start Payara Server)
1. Command Prompt (cmd) හෝ PowerShell එකක් open කර Payara bin ෆෝල්ඩරයට යන්න:
   ```powershell
   cd D:\payara6\bin
   ```
2. පහත command එක භාවිතයෙන් Payara Server (domain1) ආරම්භ කරන්න:
   ```powershell
   .\asadmin.bat start-domain domain1
   ```

---

### 3 පියවර: ව්‍යාපෘතිය ඩිප්ලෝයි කිරීම (Deploy the Application)
ව්‍යාපෘතිය Payara වෙත deploy කිරීමට ක්‍රම දෙකක් ඇත:

*   **ක්‍රමය A (Auto-Deploy - ඉතා පහසුයි)**:
    Build කරන ලද `D:\BCD1\techmart-ear\target\ecommerce-modernization.ear` ගොනුව copy කර Payara හි autodeploy ෆෝල්ඩරය වන `D:\payara6\glassfish\domains\domain1\autodeploy` තුළට paste කරන්න.
*   **ක්‍රමය B (Admin Console හරහා)**:
    1. බ්‍රවුසර් එකෙන් Payara Admin Console එක වන **[http://localhost:4848/](http://localhost:4848/)** වෙත යන්න.
    2. වම්පස ඇති **Applications** tab එක තෝරා **Deploy** බොත්තම ක්ලික් කරන්න.
    3. `ecommerce-modernization.ear` ගොනුව තෝරා deploy කරන්න.

---

### 4 පියවර: වෙබ් අඩවියට පිවිසීම (Access the Web UI)
ඩිප්ලෝයි වීම සාර්ථකව අවසන් වූ පසු, බ්‍රවුසර් එකක් හරහා පහත ලිපිනයෙන් වෙබ් අඩවියට පිවිසෙන්න:
👉 **[http://localhost:8080/ecommerce-modernization/](http://localhost:8080/ecommerce-modernization/)**

### 🧪 API සහ ඩේටාබේස් Seed කිරීම
පළමු වරට ධාවනය කිරීමේදී, පද්ධතියට Sample Products ඇතුලත් කරගැනීමට පහත සබැඳිය ක්‍රියාත්මක කරන්න (Seed API):
👉 **[http://localhost:8080/ecommerce-modernization/api/seed](http://localhost:8080/ecommerce-modernization/api/seed)**

---

### 🛑 Payara Server එක Stop කිරීමට (Stop Payara Server)
වෙබ් අඩවිය අක්‍රීය කිරීමට අවශ්‍ය නම්, `D:\payara6\bin` තුළ සිට පහත command එක run කරන්න:
```powershell
.\asadmin.bat stop-domain domain1
```
