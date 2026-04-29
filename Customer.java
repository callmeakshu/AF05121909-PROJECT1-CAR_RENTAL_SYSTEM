package com.carrental.dao;

import com.carrental.db.DBConnection;
import com.carrental.model.Booking;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking DAO.
 * <p>
 * The {@link #createBooking(Booking)} method runs in a single transaction
 * with row-level locking on the car ({@code SELECT ... FOR UPDATE}) and a
 * conflict check against existing bookings, so two concurrent users cannot
 * double-book the same car for overlapping dates.
 */
public class BookingDAO {

    public long createBooking(Booking b) throws SQLException {
        if (b.getEndDate().isBefore(b.getStartDate()))
            throw new IllegalArgumentException("End date must be on/after start date");

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                // 1) Lock the car row to serialise concurrent booking attempts.
                String carSql = "SELECT car_id, daily_rate, status FROM cars WHERE car_id=? FOR UPDATE";
                BigDecimal rate;
                String status;
                try (PreparedStatement ps = c.prepareStatement(carSql)) {
                    ps.setInt(1, b.getCarId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Car not found: " + b.getCarId());
                        rate   = rs.getBigDecimal("daily_rate");
                        status = rs.getString("status");
                    }
                }
                if (!"AVAILABLE".equals(status))
                    throw new SQLException("Car is not available (status=" + status + ")");

                // 2) Overlap check
                String overlapSql = """
                        SELECT booking_id FROM bookings
                        WHERE car_id = ?
                          AND status IN ('CONFIRMED','ONGOING')
                          AND NOT (end_date < ? OR start_date > ?)
                        LIMIT 1
                        """;
                try (PreparedStatement ps = c.prepareStatement(overlapSql)) {
                    ps.setInt(1, b.getCarId());
                    ps.setDate(2, Date.valueOf(b.getStartDate()));
                    ps.setDate(3, Date.valueOf(b.getEndDate()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException(
                                "Car already booked for this date range (conflict booking #" +
                                rs.getLong(1) + ")");
                    }
                }

                // 3) Compute cost (inclusive)
                int days = (int) ChronoUnit.DAYS.between(b.getStartDate(), b.getEndDate()) + 1;
                BigDecimal total = rate.multiply(BigDecimal.valueOf(days));
                b.setDays(days);
                b.setDailyRate(rate);
                b.setTotalCost(total);
                b.setStatus(Booking.Status.CONFIRMED);

                // 4) Insert
                String ins = "INSERT INTO bookings (car_id, customer_id, start_date, end_date, " +
                             "days, daily_rate, total_cost, status, notes) " +
                             "VALUES (?,?,?,?,?,?,?,?,?)";
                long id;
                try (PreparedStatement ps = c.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, b.getCarId());
                    ps.setInt(2, b.getCustomerId());
                    ps.setDate(3, Date.valueOf(b.getStartDate()));
                    ps.setDate(4, Date.valueOf(b.getEndDate()));
                    ps.setInt(5, days);
                    ps.setBigDecimal(6, rate);
                    ps.setBigDecimal(7, total);
                    ps.setString(8, b.getStatus().name());
                    ps.setString(9, b.getNotes());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No booking_id generated");
                        id = rs.getLong(1);
                    }
                }
                b.setId(id);

                c.commit();
                return id;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public boolean updateStatus(long bookingId, Booking.Status status) throws SQLException {
        String sql = "UPDATE bookings SET status=? WHERE booking_id=?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, bookingId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Cancel only if currently CONFIRMED and start_date is in the future. */
    public boolean cancel(long bookingId) throws SQLException {
        String sql = "UPDATE bookings SET status='CANCELLED' " +
                     "WHERE booking_id=? AND status='CONFIRMED' AND start_date > CURDATE()";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            return ps.executeUpdate() > 0;
        }
    }

    public Booking findById(long id) throws SQLException {
        String sql = """
                SELECT b.*,
                       CONCAT(c.make,' ',c.model,' (',c.registration_no,')') AS car_desc,
                       cu.name AS customer_name
                FROM bookings b
                JOIN cars c       ON c.car_id      = b.car_id
                JOIN customers cu ON cu.customer_id = b.customer_id
                WHERE b.booking_id = ?
                """;
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Booking> listAll() throws SQLException {
        String sql = """
                SELECT b.*,
                       CONCAT(c.make,' ',c.model,' (',c.registration_no,')') AS car_desc,
                       cu.name AS customer_name
                FROM bookings b
                JOIN cars c       ON c.car_id      = b.car_id
                JOIN customers cu ON cu.customer_id = b.customer_id
                ORDER BY b.start_date DESC
                """;
        List<Booking> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Booking> listByCustomer(int customerId) throws SQLException {
        String sql = """
                SELECT b.*,
                       CONCAT(c.make,' ',c.model,' (',c.registration_no,')') AS car_desc,
                       cu.name AS customer_name
                FROM bookings b
                JOIN cars c       ON c.car_id      = b.car_id
                JOIN customers cu ON cu.customer_id = b.customer_id
                WHERE b.customer_id = ?
                ORDER BY b.start_date DESC
                """;
        List<Booking> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<Booking> listByCar(int carId) throws SQLException {
        String sql = """
                SELECT b.*,
                       CONCAT(c.make,' ',c.model,' (',c.registration_no,')') AS car_desc,
                       cu.name AS customer_name
                FROM bookings b
                JOIN cars c       ON c.car_id      = b.car_id
                JOIN customers cu ON cu.customer_id = b.customer_id
                WHERE b.car_id = ?
                ORDER BY b.start_date DESC
                """;
        List<Booking> out = new ArrayList<>();
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, carId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /**
     * Bulk status sweep: any CONFIRMED booking whose start date has arrived is
     * promoted to ONGOING; any ONGOING booking whose end date has passed is
     * marked COMPLETED. Uses {@code addBatch()} for batch processing.
     */
    public int[] sweepStatuses() throws SQLException {
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement()) {
            st.addBatch("UPDATE bookings SET status='ONGOING' " +
                        "WHERE status='CONFIRMED' AND start_date <= CURDATE() AND end_date >= CURDATE()");
            st.addBatch("UPDATE bookings SET status='COMPLETED' " +
                        "WHERE status IN ('CONFIRMED','ONGOING') AND end_date < CURDATE()");
            return st.executeBatch();
        }
    }

    private Booking map(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getLong("booking_id"));
        b.setCarId(rs.getInt("car_id"));
        b.setCustomerId(rs.getInt("customer_id"));
        b.setStartDate(rs.getDate("start_date").toLocalDate());
        b.setEndDate(rs.getDate("end_date").toLocalDate());
        b.setDays(rs.getInt("days"));
        b.setDailyRate(rs.getBigDecimal("daily_rate"));
        b.setTotalCost(rs.getBigDecimal("total_cost"));
        b.setStatus(Booking.Status.valueOf(rs.getString("status")));
        b.setNotes(rs.getString("notes"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        // joined fields if present
        try { b.setCarDescription(rs.getString("car_desc")); }       catch (SQLException ignored) { }
        try { b.setCustomerName  (rs.getString("customer_name")); }  catch (SQLException ignored) { }
        return b;
    }

    /**
     * Quick availability check (read-only). Useful for UI before user commits.
     */
    public boolean isAvailable(int carId, LocalDate start, LocalDate end) throws SQLException {
        String sql = """
                SELECT 1 FROM bookings
                WHERE car_id = ?
                  AND status IN ('CONFIRMED','ONGOING')
                  AND NOT (end_date < ? OR start_date > ?)
                LIMIT 1
                """;
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }
}
