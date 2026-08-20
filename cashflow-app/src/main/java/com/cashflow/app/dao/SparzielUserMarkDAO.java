package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;

public class SparzielUserMarkDAO {

    public boolean exists(int sparzielId, int userId, LocalDate rate) throws SQLException {
        String sql = "SELECT 1 FROM SparzielUserMark WHERE sparziel_id=? AND user_id=? AND rate_datum=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, sparzielId);
            ps.setInt(2, userId);
            ps.setString(3, rate.toString());
            try (ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    public void upsert(int sparzielId, int userId, LocalDate rate) throws SQLException {
        if (exists(sparzielId, userId, rate)) return;
        String sql = "INSERT INTO SparzielUserMark (sparziel_id, user_id, rate_datum) VALUES (?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, sparzielId);
            ps.setInt(2, userId);
            ps.setString(3, rate.toString());
            ps.executeUpdate();
        }
    }

    public void clearForDate(int sparzielId, LocalDate rate) throws SQLException {
        String sql = "DELETE FROM SparzielUserMark WHERE sparziel_id=? AND rate_datum=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, sparzielId);
            ps.setString(2, rate.toString());
            ps.executeUpdate();
        }
    }
}
