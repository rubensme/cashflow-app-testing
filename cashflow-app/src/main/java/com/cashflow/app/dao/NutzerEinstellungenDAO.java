package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.NutzerEinstellungen;

import java.sql.*;

public class NutzerEinstellungenDAO {

    /** Cria (se não existir) e retorna as configurações do usuário. */
    public NutzerEinstellungen getByNutzerId(int nutzerId) {
        ensureDefaultRow(nutzerId); // garante existência com include_cash=1
        String sql = "SELECT nutzer_id, include_cash FROM nutzer_einstellungen WHERE nutzer_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean includeCash = rs.getInt("include_cash") == 1;
                    return new NutzerEinstellungen(nutzerId, includeCash);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // fallback (não deveria acontecer)
        return new NutzerEinstellungen(nutzerId, true);
    }

    /** Atualiza (upsert) a flag include_cash do usuário. */
    public void updateIncludeCash(int nutzerId, boolean includeCash) {
        String sql = "INSERT INTO nutzer_einstellungen (nutzer_id, include_cash) VALUES (?, ?) " +
                     "ON CONFLICT(nutzer_id) DO UPDATE SET include_cash = excluded.include_cash";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            stmt.setInt(2, includeCash ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Garante que existe uma linha padrão para o usuário (include_cash = 1). */
    public void ensureDefaultRow(int nutzerId) {
        String sql = "INSERT OR IGNORE INTO nutzer_einstellungen (nutzer_id, include_cash) VALUES (?, 1)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
