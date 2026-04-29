# Car Rental Management System (Java + JDBC)

A console-based car rental management system built in **Core Java + JDBC**, demonstrating:

- **JDBC PreparedStatements** (SQL-injection safe everywhere)
- **Transaction management** for booking creation (commit / rollback)
- **Row-level locking** (`SELECT … FOR UPDATE`) on the car row to prevent **double-booking** under concurrency
- **Overlap detection** for date ranges (pure SQL — no manual loops)
- **Automatic cost calculation** based on rental duration
- **Batch processing** for booking status sweeps
- **Database indexing** on hot columns
- **Role-based access control** (Admin / Customer)

---

## 1. Project Structure

```
car-rental-system/
├── sql/
│   └── schema.sql                      -- DB schema + seed data (MySQL)
├── src/main/java/com/carrental/
│   ├── Main.java                       -- entry point + login loop
│   ├── db/DBConnection.java            -- JDBC connection factory
│   ├── model/                          -- POJOs (Car, Customer, Booking, Payment, User)
│   ├── dao/                            -- DAOs (Car, Customer, Booking, Payment, User, Report)
│   ├── service/
│   │   ├── AuthService.java            -- login / role
│   │   └── RentalService.java          -- search, book, pay, sweep
│   ├── ui/                             -- AdminMenu, CustomerMenu
│   └── util/ConsoleIO.java             -- input helpers
├── src/main/resources/
│   └── db.properties                   -- JDBC URL, user, password
├── lib/                                -- drop the JDBC driver jar here
├── build.sh / build.bat                -- compile + run (no Maven needed)
└── pom.xml                             -- optional Maven build
```

---

## 2. Setup

### 2.1 Install database & load schema

**MySQL** (default):

```bash
mysql -u root -p < sql/schema.sql
```

This creates the database `car_rental_db`, all tables, indexes, and seed data (1 admin, 2 customers, 6 cars).

**PostgreSQL:** the schema is mostly compatible. Replace the `ENUM` columns with check-constraints and tweak `CURDATE()` -> `CURRENT_DATE`.

### 2.2 Configure connection

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/car_rental_db?useSSL=false&serverTimezone=UTC
db.user=root
db.password=YOUR_PASSWORD
db.driver=com.mysql.cj.jdbc.Driver
```

### 2.3 Add the JDBC driver

Download the driver jar and put it in `lib/`:

- MySQL → `mysql-connector-j-8.4.0.jar` ([download](https://dev.mysql.com/downloads/connector/j/))
- PostgreSQL → `postgresql-42.7.4.jar` ([download](https://jdbc.postgresql.org/download/))

---

## 3. Build & Run

### Option A — plain `javac` (no build tool)

Linux / macOS:

```bash
chmod +x build.sh
./build.sh
```

Windows:

```bat
build.bat
```

### Option B — Maven

```bash
mvn clean package
java -cp "target/car-rental-1.0.0.jar:lib/*" com.carrental.Main
```

---

## 4. Default Credentials (from seed)

| Username | Password    | Role     |
| -------- | ----------- | -------- |
| admin    | admin123    | ADMIN    |
| alice    | alice123    | CUSTOMER |
| bob      | bob123      | CUSTOMER |

> Passwords are stored in plaintext for demo purposes only. In production, hash them (BCrypt / Argon2).

---

## 5. Feature Tour

### Admin
- Manage cars (add / update / update rate / update status / delete / search)
- Manage customers
- View **all** bookings; manually update booking status
- **Sweep** booking statuses (CONFIRMED → ONGOING → COMPLETED) in one batch
- Reports: total revenue, revenue per car, status breakdown
- Manage user accounts and activation

### Customer
- Browse all cars
- **Search available cars by pickup/return date** (date-range query; only cars without overlapping bookings shown)
- Book a car (real-time availability; cost auto-computed = days × daily rate)
- View own bookings
- Cancel a CONFIRMED, future-dated booking
- Make payments (CASH / CARD / UPI / NETBANKING) and view payment history per booking

---

## 6. Where the “advanced concepts” live

| Concept                          | File / Method                                             |
| -------------------------------- | --------------------------------------------------------- |
| Transactions (commit / rollback) | `BookingDAO.createBooking`                                |
| Row-level locking                | `BookingDAO.createBooking` (`SELECT … FOR UPDATE` on car) |
| Overlap detection (pure SQL)     | `CarDAO.findAvailable`, `BookingDAO.isAvailable`          |
| Auto cost calculation            | `RentalService.estimateCost`, `BookingDAO.createBooking`  |
| Batch processing                 | `BookingDAO.sweepStatuses` (`addBatch / executeBatch`)    |
| Prepared statements              | every DAO method                                          |
| Indexes                          | `sql/schema.sql` (`idx_*`)                                |
| Role-based UI                    | `Main.java` switch → Admin / Customer menu                |
| Aggregated reports               | `ReportDAO.revenuePerCar / totalRevenue / statusBreakdown`|

---

## 7. Sample Session

```
==============================================
   Car Rental Management System  (Java + JDBC)
==============================================
------------------------------------------------------------
  LOGIN
------------------------------------------------------------
Username (or 'exit'): alice
Password: alice123
Welcome, Alice Johnson (CUSTOMER)
------------------------------------------------------------
  CUSTOMER MENU  (Hi, Alice Johnson)
------------------------------------------------------------
1) Browse all cars
2) Search available cars by date
3) Book a car
4) My bookings
5) Cancel a booking
6) Make a payment
7) View payments for a booking
0) Logout
> 2
Pickup date (YYYY-MM-DD): 2026-05-10
Return date (YYYY-MM-DD): 2026-05-13
Available for 4 day(s):
[1] KA01AB1234   Maruti   Swift      2023 | Hatchback MANUAL    PETROL 5s | ₹1500/day | AVAILABLE   => Total ₹6000
[4] DL03GH3456   Honda    City       2023 | Sedan     AUTOMATIC PETROL 5s | ₹2200/day | AVAILABLE   => Total ₹8800
[2] KA01CD5678   Hyundai  Creta      2022 | SUV       AUTOMATIC PETROL 5s | ₹2800/day | AVAILABLE   => Total ₹11200
...
> 3
Car id to book: 4
Pickup date (YYYY-MM-DD): 2026-05-10
Return date (YYYY-MM-DD): 2026-05-13
Estimated cost: 4 day(s) x ₹2200 = ₹8800
Confirm booking? (y/n): y
Notes (optional): need GPS

=========== BOOKING CONFIRMED ===========
Booking #1 | 2026-05-10 -> 2026-05-13 (4 days) | Honda City (DL03GH3456) | Alice Johnson | ₹8800.00 | CONFIRMED
=========================================
```

A second concurrent attempt to book the same car for an overlapping window fails cleanly with:

```
Booking failed: Car already booked for this date range (conflict booking #1)
```

---

## 8. Notes

- Compiles with **JDK 17+** (uses text blocks and records).
- No external dependencies besides the JDBC driver — everything else is plain Core Java.
- DAOs are explicit so each JDBC call is visible, rather than hidden behind an ORM.
