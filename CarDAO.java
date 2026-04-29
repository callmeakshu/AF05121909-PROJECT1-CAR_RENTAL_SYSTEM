-- =====================================================================
-- Car Rental Management System  ::  Schema (MySQL 8 / MariaDB)
-- =====================================================================
-- Run:  mysql -u root -p < schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS car_rental_db;
CREATE DATABASE car_rental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE car_rental_db;

-- ---------------------------------------------------------------------
-- Users (login accounts: ADMIN / CUSTOMER)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id      INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(60)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,             -- hash in production
    full_name    VARCHAR(120) NOT NULL,
    role         ENUM('ADMIN','CUSTOMER') NOT NULL,
    active       BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_users_role ON users(role);

-- ---------------------------------------------------------------------
-- Customers (profile linked to a user account)
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    customer_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NULL UNIQUE,
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(120) NOT NULL UNIQUE,
    phone          VARCHAR(20)  NOT NULL,
    license_no     VARCHAR(40)  NOT NULL UNIQUE,
    address        VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cust_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Cars
-- ---------------------------------------------------------------------
CREATE TABLE cars (
    car_id          INT AUTO_INCREMENT PRIMARY KEY,
    registration_no VARCHAR(20) NOT NULL UNIQUE,
    make            VARCHAR(50) NOT NULL,
    model           VARCHAR(50) NOT NULL,
    year            INT NOT NULL,
    category        VARCHAR(40),                    -- SUV / Sedan / Hatchback ...
    transmission    ENUM('MANUAL','AUTOMATIC') DEFAULT 'MANUAL',
    fuel_type       ENUM('PETROL','DIESEL','EV','HYBRID') DEFAULT 'PETROL',
    seats           INT DEFAULT 5,
    daily_rate      DECIMAL(10,2) NOT NULL CHECK (daily_rate >= 0),
    status          ENUM('AVAILABLE','MAINTENANCE','RETIRED') DEFAULT 'AVAILABLE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_cars_status   ON cars(status);
CREATE INDEX idx_cars_category ON cars(category);

-- ---------------------------------------------------------------------
-- Bookings  (a booking blocks the car for a date range)
--   status:  CONFIRMED -> ONGOING -> COMPLETED  (or CANCELLED)
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    car_id         INT NOT NULL,
    customer_id    INT NOT NULL,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    days           INT  NOT NULL,
    daily_rate     DECIMAL(10,2) NOT NULL,
    total_cost     DECIMAL(12,2) NOT NULL,
    status         ENUM('CONFIRMED','ONGOING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    notes          VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bk_car  FOREIGN KEY (car_id)
        REFERENCES cars(car_id)         ON DELETE RESTRICT,
    CONSTRAINT fk_bk_cust FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT chk_bk_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB;

CREATE INDEX idx_bk_car_date  ON bookings(car_id, start_date, end_date);
CREATE INDEX idx_bk_customer  ON bookings(customer_id);
CREATE INDEX idx_bk_status    ON bookings(status);

-- ---------------------------------------------------------------------
-- Payments
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    payment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id   BIGINT NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    method       ENUM('CASH','CARD','UPI','NETBANKING') NOT NULL,
    paid_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reference_no VARCHAR(60),
    CONSTRAINT fk_pay_booking FOREIGN KEY (booking_id)
        REFERENCES bookings(booking_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_pay_booking ON payments(booking_id);

-- ---------------------------------------------------------------------
-- Seed data
-- ---------------------------------------------------------------------
-- Default admin: admin / admin123
INSERT INTO users (username, password, full_name, role) VALUES
 ('admin', 'admin123', 'System Administrator', 'ADMIN'),
 ('alice', 'alice123', 'Alice Johnson',        'CUSTOMER'),
 ('bob',   'bob123',   'Bob Singh',            'CUSTOMER');

INSERT INTO customers (user_id, name, email, phone, license_no, address) VALUES
 (2, 'Alice Johnson', 'alice@example.com', '9876500001', 'DL-AJ-001', '14 Park Lane, Bengaluru'),
 (3, 'Bob Singh',     'bob@example.com',   '9876500002', 'DL-BS-002', '88 MG Road, Mumbai');

INSERT INTO cars (registration_no, make, model, year, category, transmission, fuel_type, seats, daily_rate) VALUES
 ('KA01AB1234', 'Maruti',   'Swift',   2023, 'Hatchback', 'MANUAL',    'PETROL', 5, 1500.00),
 ('KA01CD5678', 'Hyundai',  'Creta',   2022, 'SUV',       'AUTOMATIC', 'PETROL', 5, 2800.00),
 ('MH02EF9012', 'Toyota',   'Innova',  2024, 'MUV',       'MANUAL',    'DIESEL', 7, 3500.00),
 ('DL03GH3456', 'Honda',    'City',    2023, 'Sedan',     'AUTOMATIC', 'PETROL', 5, 2200.00),
 ('KA01IJ7890', 'Tata',     'Nexon EV',2024, 'SUV',       'AUTOMATIC', 'EV',     5, 3000.00),
 ('TN04KL1122', 'Mahindra', 'Thar',    2023, 'SUV',       'MANUAL',    'DIESEL', 4, 4000.00);
