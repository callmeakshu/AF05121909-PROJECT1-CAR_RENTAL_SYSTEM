package com.carrental.dao;

import com.carrental.db.DBConnection;
import com.carrental.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public long add(Payment p) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, amount, method, reference_no) VALUES (?,?,?,?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getBookingId());
            ps.setBigDecimal(2, p.getAmount());
            ps.setString(3, p.getMethod().name());
            ps.setString(4, p.getReferenceNo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public List<Payment> listForBooking(long bookingId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE booking_id=? ORDER BY paid_at";
        List<Payment> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public BigDecimal totalPaid(long bookingId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE booking_id=?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        }
        return BigDecimal.ZERO;
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getLong("payment_id"));
        p.setBookingId(rs.getLong("booking_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setMethod(Payment.Method.valueOf(rs.getString("method")));
        p.setPaidAt(rs.getTimestamp("paid_at"));
        p.setReferenceNo(rs.getString("reference_no"));
        return p;
    }
}
