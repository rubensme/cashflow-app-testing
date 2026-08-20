package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.Nutzer;

import java.sql.*;

public class NutzerDAO {

    // ===== CREATE =====
    public void save(Nutzer nutzer) throws SQLException {
        // exige que a coluna mailbox exista (ALTER TABLE ... ADD COLUMN mailbox TEXT)
        final String sql = "INSERT INTO Nutzer (vorname, nachname, email, passwort, mailbox) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nutzer.getVorname());
            stmt.setString(2, nutzer.getNachname());
            stmt.setString(3, nutzer.getEmail());
            stmt.setString(4, nutzer.getPasswort());
            stmt.setString(5, ""); // começa vazio

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Fehler beim Einfügen des Nutzers, keine Zeile betroffen.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) nutzer.setNutzerId(rs.getInt(1));
                else throw new SQLException("Fehler beim Abrufen der generierten Nutzer-ID.");
            }
        }
    }

    // ===== READ =====
    public Nutzer findByEmail(String email) throws SQLException {
        final String sql = "SELECT nutzer_id, vorname, nachname, email, passwort FROM Nutzer WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Nutzer n = new Nutzer();
                    n.setNutzerId(rs.getInt("nutzer_id"));
                    n.setVorname(rs.getString("vorname"));
                    n.setNachname(rs.getString("nachname"));
                    n.setEmail(rs.getString("email"));
                    n.setPasswort(rs.getString("passwort"));
                    return n;
                }
            }
        }
        return null;
    }

    public Nutzer findById(int nutzerId) throws SQLException {
        final String sql = "SELECT nutzer_id, vorname, nachname, email, passwort FROM Nutzer WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, nutzerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Nutzer n = new Nutzer();
                    n.setNutzerId(rs.getInt("nutzer_id"));
                    n.setVorname(rs.getString("vorname"));
                    n.setNachname(rs.getString("nachname"));
                    n.setEmail(rs.getString("email"));
                    n.setPasswort(rs.getString("passwort"));
                    return n;
                }
            }
        }
        return null;
    }

    // ===== UPDATE (dados básicos) =====
    public void update(Nutzer nutzer) throws SQLException {
        final String sql = "UPDATE Nutzer SET vorname = ?, nachname = ?, email = ?, passwort = ? WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nutzer.getVorname());
            stmt.setString(2, nutzer.getNachname());
            stmt.setString(3, nutzer.getEmail());
            stmt.setString(4, nutzer.getPasswort());
            stmt.setInt(5, nutzer.getNutzerId());
            stmt.executeUpdate();
        }
    }

    // ===== DELETE =====
    public void deleteById(int nutzerId) throws SQLException {
        final String sql = "DELETE FROM Nutzer WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, nutzerId);
            stmt.executeUpdate();
        }
    }

    // ===== MAILBOX (convites) =====
    public String getMailbox(int nutzerId) throws SQLException {
        final String sql = "SELECT mailbox FROM Nutzer WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("mailbox");
            }
        }
        return null; // trate como vazio no uso
    }

    public void setMailbox(int nutzerId, String content) throws SQLException {
        final String sql = "UPDATE Nutzer SET mailbox = ? WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, content == null ? "" : content);
            ps.setInt(2, nutzerId);
            ps.executeUpdate();
        }
    }
    

    /** Acrescenta uma linha à mailbox (separador '\n'). Evita null. */
    public void appendMailboxLine(int nutzerId, String line) throws SQLException {
        if (line == null || line.isBlank()) return;
        String current = getMailbox(nutzerId);
        if (current == null || current.isBlank()) {
            setMailbox(nutzerId, line);
        } else {
            setMailbox(nutzerId, current + "\n" + line);
        }
    }
    
 // Acrescenta uma linha ao mailbox (texto simples, linhas separadas por \n)
    public void appendToMailbox(int nutzerId, String message) throws SQLException {
        final String sql =
            "UPDATE Nutzer " +
            "SET mailbox = CASE " +
            "  WHEN mailbox IS NULL OR mailbox = '' THEN ? " +
            "  ELSE mailbox || CHAR(10) || ? " +
            "END " +
            "WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setString(2, message);
            ps.setInt(3, nutzerId);
            ps.executeUpdate();
        }
    }


    /** Remove exatamente a linha informada (match exato). */
    public void removeMailboxLine(int nutzerId, String exactLine) throws SQLException {
        String current = getMailbox(nutzerId);
        if (current == null || current.isBlank()) return;

        StringBuilder sb = new StringBuilder();
        for (String l : current.split("\\R")) {
            if (!l.equals(exactLine)) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(l);
            }
        }
        setMailbox(nutzerId, sb.toString());
    }

    public void clearMailbox(int nutzerId) throws SQLException {
        setMailbox(nutzerId, "");
    }
}
