package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.Nutzer;
import com.cashflow.app.model.Sparziel;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SparzielDAO {

    // 🔹 Inserir novo Sparziel
    public void save(Sparziel sparziel) throws SQLException {
        String sql = "INSERT INTO sparziel (nutzer_id, titel, ziel_betrag, ziel_datum, beschreibung, aktueller_betrag) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, sparziel.getNutzer().getNutzerId());
            stmt.setString(2, sparziel.getTitel());
            stmt.setDouble(3, sparziel.getZielBetrag());
            stmt.setString(4, sparziel.getFaelligBis().toString());
            stmt.setString(5, sparziel.getBeschreibung());
            stmt.setDouble(6, sparziel.getAktuellerBetrag());

            int rows = stmt.executeUpdate();
            if (rows == 0) throw new SQLException("Fehler beim Einfügen des Sparziels.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    sparziel.setSparzielId(rs.getInt(1));
                }
            }
        }
    }

    // 🔹 Buscar todos os Sparziele de um usuário
    public List<Sparziel> findByNutzerId(int nutzerId) throws SQLException {
        String sql = "SELECT * FROM sparziel WHERE nutzer_id = ?";
        List<Sparziel> sparziele = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Sparziel s = new Sparziel();
                s.setSparzielId(rs.getInt("sparziel_id"));
                s.setTitel(rs.getString("titel"));
                s.setZielBetrag(rs.getDouble("ziel_betrag"));
                s.setFaelligBis(LocalDate.parse(rs.getString("ziel_datum")));
                s.setBeschreibung(rs.getString("beschreibung"));
                s.setAktuellerBetrag(rs.getDouble("aktueller_betrag"));

                Nutzer nutzer = new Nutzer();
                nutzer.setNutzerId(rs.getInt("nutzer_id"));
                s.setNutzer(nutzer);

                sparziele.add(s);
            }
        }

        return sparziele;
    }

    // 🔹 Atualizar o valor atual poupado
    public void updateAktuellerBetrag(int sparzielId, double neuerBetrag) throws SQLException {
        String sql = "UPDATE sparziel SET aktueller_betrag = ? WHERE sparziel_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, neuerBetrag);
            stmt.setInt(2, sparzielId);
            stmt.executeUpdate();
        }
    }

    // 🔹 Deletar por ID
    public void deleteById(int sparzielId) throws SQLException {
        String sql = "DELETE FROM sparziel WHERE sparziel_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sparzielId);
            stmt.executeUpdate();
        }
    }
}
