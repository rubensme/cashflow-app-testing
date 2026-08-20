package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.SparzielPlan;

import java.sql.*;
import java.time.LocalDate;

public class SparzielPlanDAO {

    public void save(SparzielPlan p) throws SQLException {
        String sql = """
            INSERT INTO SparzielPlan (sparziel_id, periode, mitglieder_anzahl, beitrag_pro_mitglied, beitrag_pro_gruppe, naechste_rate, letzte_rate)
            VALUES (?,?,?,?,?,?,?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getSparzielId());
            ps.setString(2, p.getPeriode());
            ps.setInt(3, p.getMitgliederAnzahl());
            ps.setDouble(4, p.getBeitragProMitglied());
            ps.setDouble(5, p.getBeitragProGruppe());
            ps.setString(6, p.getNaechsteRate().toString());
            ps.setString(7, p.getLetzteRate() == null ? null : p.getLetzteRate().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()){
                if (rs.next()) p.setPlanId(rs.getInt(1));
            }
        }
    }

    public SparzielPlan findBySparzielId(int sparzielId) throws SQLException {
        String sql = "SELECT * FROM SparzielPlan WHERE sparziel_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sparzielId);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    SparzielPlan p = new SparzielPlan();
                    p.setPlanId(rs.getInt("plan_id"));
                    p.setSparzielId(rs.getInt("sparziel_id"));
                    p.setPeriode(rs.getString("periode"));
                    p.setMitgliederAnzahl(rs.getInt("mitglieder_anzahl"));
                    p.setBeitragProMitglied(rs.getDouble("beitrag_pro_mitglied"));
                    p.setBeitragProGruppe(rs.getDouble("beitrag_pro_gruppe"));
                    p.setNaechsteRate(LocalDate.parse(rs.getString("naechste_rate")));
                    String lr = rs.getString("letzte_rate");
                    p.setLetzteRate(lr == null ? null : LocalDate.parse(lr));
                    return p;
                }
            }
        }
        return null;
    }

    public void advanceToNextRate(int sparzielId, LocalDate newNext, LocalDate last) throws SQLException {
        String sql = "UPDATE SparzielPlan SET naechste_rate = ?, letzte_rate = ? WHERE sparziel_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)){
            ps.setString(1, newNext.toString());
            ps.setString(2, last == null ? null : last.toString());
            ps.setInt(3, sparzielId);
            ps.executeUpdate();
        }
    }

    public void updateCurrentAmounts(int sparzielId, double newMitglied, double newGruppe) throws SQLException {
        String sql = "UPDATE SparzielPlan SET beitrag_pro_mitglied = ?, beitrag_pro_gruppe = ? WHERE sparziel_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)){
            ps.setDouble(1, newMitglied);
            ps.setDouble(2, newGruppe);
            ps.setInt(3, sparzielId);
            ps.executeUpdate();
        }
    }
}
