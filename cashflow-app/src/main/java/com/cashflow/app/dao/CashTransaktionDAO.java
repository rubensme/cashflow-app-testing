package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.CashTransaktion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CashTransaktionDAO {

    // Inserir nova transação de Cash
    public void save(CashTransaktion cash) throws SQLException {
        String sql = "INSERT INTO CashTransaktion (datum, beschreibung, betrag, nutzer_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cash.getDatum());
            stmt.setString(2, cash.getBeschreibung());
            stmt.setDouble(3, cash.getBetrag());
            stmt.setInt(4, cash.getNutzerId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Fehler beim Einfügen der Cash-Transaktion.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cash.setCashTransaktionId(rs.getInt(1));
                }
            }
        }
    }

    // Somatório do saldo em Cash do usuário (entradas positivas, saídas negativas)
    public double sumCashSaldoByNutzerId(int nutzerId) {
        String sql = "SELECT COALESCE(SUM(betrag), 0) AS saldo FROM CashTransaktion WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("saldo");
                }
            }
        } catch (SQLException e) {
            // Log simples; ajuste se tiver logger
            e.printStackTrace();
        }
        return 0.0;
    }

    // (Opcional) Listar todas as transações de Cash do usuário – útil para tela futura
    public List<CashTransaktion> findAllByNutzerId(int nutzerId) throws SQLException {
        String sql = "SELECT cash_transaktion_id, datum, beschreibung, betrag, nutzer_id " +
                     "FROM CashTransaktion WHERE nutzer_id = ? ORDER BY datum DESC";

        List<CashTransaktion> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CashTransaktion c = new CashTransaktion();
                    c.setCashTransaktionId(rs.getInt("cash_transaktion_id"));
                    c.setDatum(rs.getString("datum"));
                    c.setBeschreibung(rs.getString("beschreibung"));
                    c.setBetrag(rs.getDouble("betrag"));
                    c.setNutzerId(rs.getInt("nutzer_id"));
                    list.add(c);
                }
            }
        }
        return list;
    }
}
