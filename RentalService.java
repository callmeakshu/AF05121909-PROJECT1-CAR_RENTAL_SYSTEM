package com.carrental.dao;

import com.carrental.db.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read-only analytics. */
public class ReportDAO {

    public record CarRevenue(int carId, String description, long bookingCount, BigDecimal revenue) { }
    public record StatusCount(String status, long count) { }

    public List<CarRevenue> revenuePerCar() throws SQLException {
        String sql = """
                SELECT c.car_id,
                       CONCAT(c.make,' ',c.model,' (',c.registration_no,')') AS desc_,
                       COUNT(b.booking_id) AS cnt,
                       COALESCE(SUM(CASE WHEN b.status <> 'CANCELLED' THEN b.total_cost ELSE 0 END),0) AS rev
                FROM cars c
                LEFT JOIN bookings b ON b.car_id = c.car_id
                GROUP BY c.car_id, desc_
                ORDER BY rev DESC
                """;
        List<CarRevenue> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new CarRevenue(
                        rs.getInt("car_id"),
                        rs.getString("desc_"),
                        rs.getLong("cnt"),
                        rs.getBigDecimal("rev")));
            }
        }
        return out;
    }

    public BigDecimal totalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_cost),0) FROM bookings WHERE status <> 'CANCELLED'";
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return BigDecimal.ZERO;
    }

    public List<StatusCount> bookingStatusBreakdown() throws SQLException {
        String sql = "SELECT status, COUNT(*) AS c FROM bookings GROUP BY status ORDER BY status";
        List<StatusCount> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new StatusCount(rs.getString("status"), rs.getLong("c")));
            }
        }
        return out;
    }
}
