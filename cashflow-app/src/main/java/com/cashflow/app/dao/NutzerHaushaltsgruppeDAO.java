package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NutzerHaushaltsgruppeDAO {

    // Adiciona um usuário a uma determinada Haushaltsgruppe
    public void nutzerZurGruppeHinzufuegen(int nutzerId, int haushaltsgruppeId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO nutzer_haushaltsgruppe (nutzer_id, haushaltsgruppe_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            stmt.setInt(2, haushaltsgruppeId);
            stmt.executeUpdate();
        }
    }

    // Remove um usuário de uma Haushaltsgruppe
    public void entferneNutzerAusGruppe(int nutzerId, int haushaltsgruppeId) throws SQLException {
        String sql = "DELETE FROM nutzer_haushaltsgruppe WHERE nutzer_id = ? AND haushaltsgruppe_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            stmt.setInt(2, haushaltsgruppeId);
            stmt.executeUpdate();
        }
    }

    // Retorna todas as Haushaltsgruppen de um usuário
    public List<Integer> findeGruppenDesNutzers(int nutzerId) throws SQLException {
        List<Integer> gruppenIds = new ArrayList<>();
        String sql = "SELECT haushaltsgruppe_id FROM nutzer_haushaltsgruppe WHERE nutzer_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gruppenIds.add(rs.getInt("haushaltsgruppe_id"));
            }
        }

        return gruppenIds;
    }

    // Retorna todos os usuários de uma Haushaltsgruppe
    public List<Integer> findeMitgliederDerGruppe(int haushaltsgruppeId) throws SQLException {
        List<Integer> nutzerIds = new ArrayList<>();
        String sql = "SELECT nutzer_id FROM nutzer_haushaltsgruppe WHERE haushaltsgruppe_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, haushaltsgruppeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                nutzerIds.add(rs.getInt("nutzer_id"));
            }
        }

        return nutzerIds;
    }
}
