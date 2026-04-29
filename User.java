package com.carrental.dao;

import com.carrental.db.DBConnection;
import com.carrental.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public int add(Customer c) throws SQLException {
        String sql = "INSERT INTO customers (user_id, name, email, phone, license_no, address) " +
                     "VALUES (?,?,?,?,?,?)";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (c.getUserId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, c.getUserId());
            ps.setString(2, c.getName());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getLicenseNo());
            ps.setString(6, c.getAddress());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) return k.getInt(1);
            }
        }
        return -1;
    }

    public boolean update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET name=?, email=?, phone=?, license_no=?, address=? WHERE customer_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getEmail());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getLicenseNo());
            ps.setString(5, c.getAddress());
            ps.setInt(6, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Customer findById(int id) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public Customer findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE user_id=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Customer> listAll() throws SQLException {
        String sql = "SELECT * FROM customers ORDER BY name";
        List<Customer> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    private Customer map(ResultSet rs) throws SQLException {
        int uid = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : uid;
        return new Customer(
                rs.getInt("customer_id"),
                userId,
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("license_no"),
                rs.getString("address"));
    }
}
