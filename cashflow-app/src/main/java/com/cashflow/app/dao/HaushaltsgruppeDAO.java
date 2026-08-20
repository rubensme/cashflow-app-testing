package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.Haushaltsgruppe;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HaushaltsgruppeDAO {

    // Cria grupo
    public Haushaltsgruppe createGroup(int ownerId, String name, String beschr, List<String> whitelist) throws SQLException {
        String emails = String.join("\n", (whitelist == null ? Collections.emptyList() : whitelist));

        String sql = "INSERT INTO haushaltsgruppe (owner_id, name, beschr, invited_emails, invites_pending) " +
                     "VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, beschr);
            ps.setString(4, emails);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    Haushaltsgruppe g = new Haushaltsgruppe();
                    g.setGruppeId(id);
                    g.setOwnerNutzerId(ownerId);
                    g.setName(name);
                    g.setBeschr(beschr);
                    g.setInvitedEmails(emails);
                    g.setInvitesPending(false);
                    return g;
                }
            }
        }
        throw new SQLException("Fehler beim Erstellen der Haushaltsgruppe.");
    }

    // Busca por ID
    public Haushaltsgruppe findById(int id) throws SQLException {
        String sql = "SELECT gruppe_id, owner_id, name, beschr, invited_emails, invites_pending " +
                     "FROM haushaltsgruppe WHERE gruppe_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // Grupos do owner com convites pendentes
    public List<Haushaltsgruppe> findPendingInvitesByOwner(int ownerId) throws SQLException {
        String sql = "SELECT gruppe_id, owner_id, name, beschr, invited_emails, invites_pending " +
                     "FROM haushaltsgruppe WHERE owner_id = ? AND invites_pending = 1 ORDER BY gruppe_id DESC";

        List<Haushaltsgruppe> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    // Owner tem algum grupo?
    public boolean ownerHasAnyGroup(int ownerId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM haushaltsgruppe WHERE owner_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        }
    }

    // Atualiza flag de pendência
    public void setInvitesPending(int gruppeId, boolean pending) throws SQLException {
        String sql = "UPDATE haushaltsgruppe SET invites_pending = ? WHERE gruppe_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pending ? 1 : 0);
            ps.setInt(2, gruppeId);
            ps.executeUpdate();
        }
    }

    // Adiciona/atualiza membro (coluna is_aktiv)
    public void addMember(int gruppeId, int nutzerId, boolean aktiv) throws SQLException {
        // Requer UNIQUE(gruppe_id, nutzer_id) em haushaltsgruppe_member para ON CONFLICT funcionar
        String sql = "INSERT INTO haushaltsgruppe_member (gruppe_id, nutzer_id, is_aktiv) " +
                     "VALUES (?, ?, ?) " +
                     "ON CONFLICT(gruppe_id, nutzer_id) DO UPDATE SET is_aktiv = excluded.is_aktiv";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            ps.setInt(2, nutzerId);
            ps.setInt(3, aktiv ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void setMemberAktiv(int gruppeId, int nutzerId, boolean aktiv) throws SQLException {
        String sql = "UPDATE haushaltsgruppe_member SET is_aktiv=? WHERE gruppe_id=? AND nutzer_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, aktiv ? 1 : 0);
            ps.setInt(2, gruppeId);
            ps.setInt(3, nutzerId);
            ps.executeUpdate();
        }
    }

    // Remove um membro específico da grupo
    public void deleteMembership(int gruppeId, int nutzerId) throws SQLException {
        String sql = "DELETE FROM haushaltsgruppe_member WHERE gruppe_id=? AND nutzer_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            ps.setInt(2, nutzerId);
            ps.executeUpdate();
        }
    }

    // Remove todos os membros de uma grupo (para deletar a grupo)
    public void deleteAllMembersByGroup(int gruppeId) throws SQLException {
        String sql = "DELETE FROM haushaltsgruppe_member WHERE gruppe_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            ps.executeUpdate();
        }
    }

    // Grupos onde o usuário é owner
    public List<Haushaltsgruppe> findOwnedBy(int ownerId) throws SQLException {
        String sql = "SELECT gruppe_id, owner_id, name, beschr, invited_emails, invites_pending " +
                     "FROM haushaltsgruppe WHERE owner_id = ? ORDER BY name";
        List<Haushaltsgruppe> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    // Grupos onde o usuário é membro (exclui os owned)
    public List<Haushaltsgruppe> findMemberOfButNotOwner(int nutzerId) throws SQLException {
        String sql =
            "SELECT g.gruppe_id, g.owner_id, g.name, g.beschr, g.invited_emails, g.invites_pending " +
            "FROM haushaltsgruppe g " +
            "JOIN haushaltsgruppe_member m ON m.gruppe_id = g.gruppe_id " +
            "WHERE m.nutzer_id = ? AND g.owner_id <> ? AND m.is_aktiv = 1 " +
            "ORDER BY g.name";
        List<Haushaltsgruppe> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            ps.setInt(2, nutzerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    // Lista de membros com status
    public List<com.cashflow.app.model.HaushaltsgruppeMitglied> findMembers(int gruppeId) throws SQLException {
        String sql = "SELECT m.gruppe_id, m.nutzer_id, m.is_aktiv, n.vorname, n.nachname, n.email " +
                     "FROM haushaltsgruppe_member m " +
                     "JOIN Nutzer n ON n.nutzer_id = m.nutzer_id " +
                     "WHERE m.gruppe_id = ? ORDER BY n.vorname, n.nachname";
        List<com.cashflow.app.model.HaushaltsgruppeMitglied> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.cashflow.app.model.HaushaltsgruppeMitglied m = new com.cashflow.app.model.HaushaltsgruppeMitglied();
                    m.setGruppeId(rs.getInt("gruppe_id"));
                    m.setNutzerId(rs.getInt("nutzer_id"));
                    m.setAktiv(rs.getInt("is_aktiv") == 1);
                    m.setVorname(rs.getString("vorname"));
                    m.setNachname(rs.getString("nachname"));
                    m.setEmail(rs.getString("email"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    // Updates simples
    public void updateName(int gruppeId, String name) throws SQLException {
        String sql = "UPDATE haushaltsgruppe SET name = ? WHERE gruppe_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, gruppeId);
            ps.executeUpdate();
        }
    }

    public void updateBeschreibung(int gruppeId, String beschr) throws SQLException {
        String sql = "UPDATE haushaltsgruppe SET beschr = ? WHERE gruppe_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, beschr);
            ps.setInt(2, gruppeId);
            ps.executeUpdate();
        }
    }

    public void updateInvitedEmails(int gruppeId, String invitedEmails) throws SQLException {
        String sql = "UPDATE haushaltsgruppe SET invited_emails = ? WHERE gruppe_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invitedEmails);
            ps.setInt(2, gruppeId);
            ps.executeUpdate();
        }
    }

    public void deleteById(int gruppeId) throws SQLException {
        String sql = "DELETE FROM haushaltsgruppe WHERE gruppe_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            ps.executeUpdate();
        }
    }

    // Mapper
    private Haushaltsgruppe mapRow(ResultSet rs) throws SQLException {
        Haushaltsgruppe g = new Haushaltsgruppe();
        g.setGruppeId(rs.getInt("gruppe_id"));
        g.setOwnerNutzerId(rs.getInt("owner_id"));
        g.setName(rs.getString("name"));
        g.setBeschr(rs.getString("beschr"));
        g.setInvitedEmails(rs.getString("invited_emails"));
        g.setInvitesPending(rs.getInt("invites_pending") == 1);
        return g;
    }
    
    public void removeMember(int gruppeId, int nutzerId) throws SQLException {
        String sql = "DELETE FROM haushaltsgruppe_member WHERE gruppe_id = ? AND nutzer_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            ps.setInt(2, nutzerId);
            ps.executeUpdate();
        }
    }

    public void deleteAllMembers(int gruppeId) throws SQLException {
        String sql = "DELETE FROM haushaltsgruppe_member WHERE gruppe_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gruppeId);
            ps.executeUpdate();
        }
    }

    
}
