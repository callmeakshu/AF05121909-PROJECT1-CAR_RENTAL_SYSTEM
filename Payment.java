package com.carrental.dao;

import com.carrental.db.DBConnection;
import com.carrental.model.Car;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CarDAO {

    public int add(Car c) throws SQLException {
        String sql = "INSERT INTO cars (registration_no, make, model, year, category, " +
                     "transmission, fuel_type, seats, daily_rate, status) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getRegistrationNo());
            ps.setString(2, c.getMake());
            ps.setString(3, c.getModel());
            ps.setInt(4,    c.getYear());
            ps.setString(5, c.getCategory());
            ps.setString(6, c.getTransmission());
            ps.setString(7, c.getFuelType());
            ps.setInt(8,    c.getSeats());
            ps.setBigDecimal(9, c.getDailyRate());
            ps.setString(10, c.getStatus() == null ? "AVAILABLE" : c.getStatus().name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) return k.getInt(1);
            }
        }
        return -1;
    }

    public boolean update(Car c) throws SQLException {
        String sql = "UPDATE cars SET registration_no=?, make=?, model=?, year=?, category=?, " +
                     "transmission=?, fuel_type=?, seats=?, daily_rate=?, status=? WHERE car_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getRegistrationNo());
            ps.setString(2, c.getMake());
            ps.setString(3, c.getModel());
            ps.setInt(4,    c.getYear());
            ps.setString(5, c.getCategory());
            ps.setString(6, c.getTransmission());
            ps.setString(7, c.getFuelType());
            ps.setInt(8,    c.getSeats());
            ps.setBigDecimal(9, c.getDailyRate());
            ps.setString(10, c.getStatus().name());
            ps.setInt(11, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateRate(int carId, BigDecimal rate) throws SQLException {
        String sql = "UPDATE cars SET daily_rate=? WHERE car_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, rate);
            ps.setInt(2, carId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int carId, Car.Status status) throws SQLException {
        String sql = "UPDATE cars SET status=? WHERE car_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, carId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int carId) throws SQLException {
        String sql = "DELETE FROM cars WHERE car_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, carId);
            return ps.executeUpdate() > 0;
        }
    }

    public Car findById(int id) throws SQLException {
        String sql = "SELECT * FROM cars WHERE car_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Car> listAll() throws SQLException {
        String sql = "SELECT * FROM cars ORDER BY make, model";
        List<Car> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    /**
     * Returns all cars that are AVAILABLE (status=AVAILABLE) AND have NO
     * overlapping booking with the supplied window. Pure SQL — no manual loop.
     */
    public List<Car> findAvailable(LocalDate start, LocalDate end) throws SQLException {
        String sql = """
                SELECT c.*
                FROM cars c
                WHERE c.status = 'AVAILABLE'
                  AND NOT EXISTS (
                        SELECT 1 FROM bookings b
                        WHERE b.car_id = c.car_id
                          AND b.status IN ('CONFIRMED','ONGOING')
                          AND NOT (b.end_date < ? OR b.start_date > ?)
                  )
                ORDER BY c.daily_rate
                """;
        List<Car> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<Car> search(String term) throws SQLException {
        String sql = "SELECT * FROM cars WHERE make LIKE ? OR model LIKE ? OR category LIKE ? OR registration_no LIKE ?";
        List<Car> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + term + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private Car map(ResultSet rs) throws SQLException {
        Car c = new Car();
        c.setId(rs.getInt("car_id"));
        c.setRegistrationNo(rs.getString("registration_no"));
        c.setMake(rs.getString("make"));
        c.setModel(rs.getString("model"));
        c.setYear(rs.getInt("year"));
        c.setCategory(rs.getString("category"));
        c.setTransmission(rs.getString("transmission"));
        c.setFuelType(rs.getString("fuel_type"));
        c.setSeats(rs.getInt("seats"));
        c.setDailyRate(rs.getBigDecimal("daily_rate"));
        c.setStatus(Car.Status.valueOf(rs.getString("status")));
        return c;
    }
}
