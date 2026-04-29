package com.carrental.dao;

import com.carrental.db.DBConnection;
import com.carrental.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, full_name, role, active " +
                     "FROM users WHERE username=? AND password=? AND active=TRUE";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public int add(User u, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password, full_name, role, active) VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, password);
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isActive());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) return k.getInt(1);
            }
        }
        return -1;
    }

    public boolean setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active=? WHERE user_id=?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> listAll() throws SQLException {
        String sql = "SELECT user_id, username, full_name, role, active FROM users ORDER BY role, username";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                User.Role.valueOf(rs.getString("role")),
                rs.getBoolean("active"));
    }
}
