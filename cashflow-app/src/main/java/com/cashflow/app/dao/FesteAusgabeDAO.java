package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.FesteAusgabe;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FesteAusgabeDAO {

    // 🔹 Inserir nova despesa fixa
    public void save(FesteAusgabe ausgabe) throws SQLException {
        String sql = "INSERT INTO feste_ausgabe (nutzer_id, titel, betrag, start_datum, rhythmus) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, ausgabe.getNutzerId());
            stmt.setString(2, ausgabe.getTitel());
            stmt.setDouble(3, ausgabe.getBetrag());
            stmt.setString(4, ausgabe.getNaechsteFaelligkeit().toString());
            stmt.setString(5, ausgabe.getRhythmus().name());

            int rows = stmt.executeUpdate();
            if (rows == 0) throw new SQLException("Fehler beim Einfügen der festen Ausgabe.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ausgabe.setFesteAusgabeID(rs.getInt(1));
                }
            }
        }
    }

    // 🔹 Buscar todas as despesas fixas de um usuário
    public List<FesteAusgabe> findByNutzerId(int nutzerId) throws SQLException {
        String sql = "SELECT * FROM feste_ausgabe WHERE nutzer_id = ?";
        List<FesteAusgabe> result = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                FesteAusgabe a = new FesteAusgabe(
                        rs.getInt("ausgabe_id"),
                        rs.getString("titel"),
                        rs.getDouble("betrag"),
                        LocalDate.parse(rs.getString("start_datum")),
                        FesteAusgabe.Rhythmus.valueOf(rs.getString("rhythmus"))
                );
                a.setNutzerId(nutzerId); // define também o nutzerId lido
                result.add(a);
            }
        }

        return result;
    }

    // 🔹 Atualizar próxima data de vencimento
    public void updateNaechsteFaelligkeit(int ausgabeId, LocalDate novaData) throws SQLException {
        String sql = "UPDATE feste_ausgabe SET start_datum = ? WHERE ausgabe_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novaData.toString());
            stmt.setInt(2, ausgabeId);
            stmt.executeUpdate();
        }
    }

    // 🔹 Remover uma despesa fixa
    public void deleteById(int ausgabeId) throws SQLException {
        String sql = "DELETE FROM feste_ausgabe WHERE ausgabe_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ausgabeId);
            stmt.executeUpdate();
        }
    }
}
