package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Transaktion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TransaktionDAO {

    // ───────────────────────────────────────────────────────────────────────────
    // CRUD básico
    // ───────────────────────────────────────────────────────────────────────────

    public void save(Transaktion t) throws SQLException {
        String sql = "INSERT INTO Transaktion (datum, betrag, titel, kategorie, konto_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, t.getDatum().toString());
            stmt.setDouble(2, t.getBetrag());
            stmt.setString(3, t.getBeschreibung());
            stmt.setString(4, t.getKategorie());

            if (t.getKonto() != null) stmt.setInt(5, t.getKonto().getBankKontoId());
            else stmt.setNull(5, Types.INTEGER);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) t.setTransaktionId(rs.getInt(1));
            }
        }
    }

    public Transaktion findById(int id) throws SQLException {
        String sql =
            "SELECT t.*, b.bankname AS quelle " +
            "FROM Transaktion t LEFT JOIN bankkonto b ON b.konto_id = t.konto_id " +
            "WHERE t.transaktion_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                Transaktion t = new Transaktion();
                t.setTransaktionId(rs.getInt("transaktion_id"));
                String ds = rs.getString("datum");
                if (ds != null && ds.length() >= 10) t.setDatum(LocalDate.parse(ds.substring(0, 10)));
                t.setBetrag(rs.getDouble("betrag"));
                t.setBeschreibung(rs.getString("titel"));
                t.setKategorie(rs.getString("kategorie"));
                String quelle = rs.getString("quelle");
                if (quelle == null || quelle.isBlank()) quelle = "Cash";
                t.setQuelle(quelle);

                Bankkonto konto = new Bankkonto();
                konto.setBankKontoId(rs.getInt("konto_id"));
                t.setKonto(konto);
                return t;
            }
        }
    }

    public List<Transaktion> findAll() throws SQLException {
        List<Transaktion> list = new ArrayList<>();
        String sql =
            "SELECT t.*, b.bankname AS quelle " +
            "FROM Transaktion t LEFT JOIN bankkonto b ON b.konto_id = t.konto_id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transaktion t = new Transaktion();
                t.setTransaktionId(rs.getInt("transaktion_id"));
                String ds = rs.getString("datum");
                if (ds != null && ds.length() >= 10) t.setDatum(LocalDate.parse(ds.substring(0, 10)));
                t.setBetrag(rs.getDouble("betrag"));
                t.setBeschreibung(rs.getString("titel"));
                t.setKategorie(rs.getString("kategorie"));
                String quelle = rs.getString("quelle");
                if (quelle == null || quelle.isBlank()) quelle = "Cash";
                t.setQuelle(quelle);

                Bankkonto konto = new Bankkonto();
                konto.setBankKontoId(rs.getInt("konto_id"));
                t.setKonto(konto);
                list.add(t);
            }
        }
        return list;
    }

    public void deleteById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Transaktion WHERE transaktion_id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // LISTAGENS para a Home — com ordenação por "sort_ts" (created_at > datum)
    // ───────────────────────────────────────────────────────────────────────────

    /** Últimas N do usuário (Transaktion + CashTransaktion). */
    public List<Transaktion> findLast10ByNutzerIdOrderByDatumDesc(int nutzerId, int limit) {
        String inner = """
            SELECT t.transaktion_id AS id,
                   t.datum               AS datum,
                   t.betrag              AS betrag,
                   t.titel               AS beschreibung,
                   t.kategorie           AS kategorie,
                   b.bankname            AS quelle,
                   t.konto_id            AS konto_id,
                   COALESCE(t.erstellt_am, t.datum) AS sort_ts
            FROM Transaktion t
            LEFT JOIN bankkonto b ON b.konto_id = t.konto_id
            WHERE b.nutzer_id = ? OR t.konto_id IS NULL

            UNION ALL

            SELECT c.cash_transaktion_id AS id,
                   c.datum               AS datum,
                   c.betrag              AS betrag,
                   c.beschreibung        AS beschreibung,
                   CASE
                     WHEN UPPER(c.beschreibung) LIKE 'MANUELLER SALDOABGLEICH%%' THEN 'AUSGLEICH'
                     ELSE 'MANUELLE TRANSAKTION'
                   END                    AS kategorie,
                   'Cash'                 AS quelle,
                   NULL                   AS konto_id,
                   COALESCE(c.erstellt_am, c.datum) AS sort_ts
            FROM CashTransaktion c
            WHERE c.nutzer_id = ?
            """;

        String sql = "SELECT * FROM (" + inner + ") x ORDER BY x.sort_ts DESC, x.datum DESC, x.id DESC LIMIT ?";

        List<Transaktion> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            ps.setInt(2, nutzerId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Transaktion> findLast5ByNutzerIdOrderByDatumDesc(int nutzerId) {
        return findLast10ByNutzerIdOrderByDatumDesc(nutzerId, 5);
    }

    /** Busca por intervalo (por NUTZER). */
    public List<Transaktion> findByNutzerIdBetweenDates(int nutzerId, LocalDate von, LocalDate bis) {
        String inner =
            "SELECT id, datum, betrag, beschreibung, kategorie, quelle, konto_id, sort_ts FROM (" +
            "  SELECT t.transaktion_id AS id, t.datum, t.betrag, t.titel AS beschreibung, " +
            "         t.kategorie AS kategorie, b.bankname AS quelle, t.konto_id AS konto_id, " +
            "         COALESCE(t.erstellt_am, t.datum) AS sort_ts " +
            "  FROM Transaktion t " +
            "  LEFT JOIN bankkonto b ON b.konto_id = t.konto_id " +
            "  WHERE (b.nutzer_id = ? OR t.konto_id IS NULL) AND DATE(t.datum) BETWEEN ? AND ? " +
            "  UNION ALL " +
            "  SELECT c.cash_transaktion_id AS id, c.datum, c.betrag, c.beschreibung, " +
            "         CASE WHEN UPPER(c.beschreibung) LIKE 'MANUELLER SALDOABGLEICH%' THEN 'AUSGLEICH' " +
            "              ELSE 'MANUELLE TRANSAKTION' END AS kategorie, " +
            "         'Cash' AS quelle, NULL AS konto_id, COALESCE(c.erstellt_am, c.datum) AS sort_ts " +
            "  FROM CashTransaktion c " +
            "  WHERE c.nutzer_id = ? AND DATE(c.datum) BETWEEN ? AND ? " +
            ") ";
        String sql = "SELECT * FROM (" + inner + ") x ORDER BY x.sort_ts DESC, x.datum DESC, x.id DESC";

        List<Transaktion> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            ps.setString(2, von.toString());
            ps.setString(3, bis.toString());
            ps.setInt(4, nutzerId);
            ps.setString(5, von.toString());
            ps.setString(6, bis.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Últimas N por contas selecionadas (inclui Cash dos mesmos Nutzer). */
    public List<Transaktion> findLastByKontenIdsOrderByDatumDesc(Set<Integer> kontenIds, int limit) {
        List<Transaktion> list = new ArrayList<>();
        if (kontenIds == null || kontenIds.isEmpty()) return list;

        StringBuilder in = new StringBuilder();
        int n = kontenIds.size();
        for (int i = 0; i < n; i++) { if (i > 0) in.append(","); in.append("?"); }

        String inner = ""
            + "SELECT t.transaktion_id AS id, t.datum, t.betrag, t.titel AS beschreibung, "
            + "       t.kategorie AS kategorie, b.bankname AS quelle, t.konto_id AS konto_id, "
            + "       COALESCE(t.erstellt_am, t.datum) AS sort_ts "
            + "FROM Transaktion t "
            + "JOIN bankkonto b ON b.konto_id = t.konto_id "
            + "WHERE t.konto_id IN (" + in + ") "
            + "UNION ALL "
            + "SELECT c.cash_transaktion_id AS id, c.datum, c.betrag, c.beschreibung, "
            + "       CASE WHEN UPPER(c.beschreibung) LIKE 'MANUELLER SALDOABGLEICH%' THEN 'AUSGLEICH' "
            + "            ELSE 'MANUELLE TRANSAKTION' END AS kategorie, "
            + "       'Cash' AS quelle, NULL AS konto_id, COALESCE(c.erstellt_am, c.datum) AS sort_ts "
            + "FROM CashTransaktion c "
            + "WHERE c.nutzer_id IN (SELECT DISTINCT nutzer_id FROM bankkonto WHERE konto_id IN (" + in + "))";

        String sql = "SELECT * FROM (" + inner + ") x ORDER BY x.sort_ts DESC, x.datum DESC, x.id DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : kontenIds) ps.setInt(idx++, id);
            for (Integer id : kontenIds) ps.setInt(idx++, id);
            ps.setInt(idx++, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Intervalo por contas selecionadas (inclui Cash). */
    public List<Transaktion> findByKontenIdsBetweenDates(Set<Integer> kontenIds, LocalDate von, LocalDate bis) {
        List<Transaktion> list = new ArrayList<>();
        if (kontenIds == null || kontenIds.isEmpty()) return list;

        StringBuilder in = new StringBuilder();
        int n = kontenIds.size();
        for (int i = 0; i < n; i++) { if (i > 0) in.append(","); in.append("?"); }

        String inner = ""
            + "SELECT t.transaktion_id AS id, t.datum, t.betrag, t.titel AS beschreibung, "
            + "       t.kategorie AS kategorie, b.bankname AS quelle, t.konto_id AS konto_id, "
            + "       COALESCE(t.erstellt_am, t.datum) AS sort_ts "
            + "FROM Transaktion t "
            + "JOIN bankkonto b ON b.konto_id = t.konto_id "
            + "WHERE t.konto_id IN (" + in + ") AND DATE(t.datum) BETWEEN ? AND ? "
            + "UNION ALL "
            + "SELECT c.cash_transaktion_id AS id, c.datum, c.betrag, c.beschreibung, "
            + "       CASE WHEN UPPER(c.beschreibung) LIKE 'MANUELLER SALDOABGLEICH%' THEN 'AUSGLEICH' "
            + "            ELSE 'MANUELLE TRANSAKTION' END AS kategorie, "
            + "       'Cash' AS quelle, NULL AS konto_id, COALESCE(c.erstellt_am, c.datum) AS sort_ts "
            + "FROM CashTransaktion c "
            + "WHERE c.nutzer_id IN (SELECT DISTINCT nutzer_id FROM bankkonto WHERE konto_id IN (" + in + ")) "
            + "  AND DATE(c.datum) BETWEEN ? AND ? ";

        String sql = "SELECT * FROM (" + inner + ") x ORDER BY x.sort_ts DESC, x.datum DESC, x.id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            for (Integer id : kontenIds) ps.setInt(idx++, id);
            ps.setString(idx++, von.toString());
            ps.setString(idx++, bis.toString());
            for (Integer id : kontenIds) ps.setInt(idx++, id);
            ps.setString(idx++, von.toString());
            ps.setString(idx++, bis.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Inserções usadas pelos controllers (com categorias)
    // ───────────────────────────────────────────────────────────────────────────

    /** Ajuste de saldo para Bankkonto (categoria AUSGLEICH). */
    public void insertManuellerAbgleichFuerKonto(int nutzerId, int kontoId, LocalDate datum, double diff, String titel) {
        final String sql = "INSERT INTO Transaktion (datum, betrag, titel, kategorie, konto_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (datum == null ? LocalDate.now() : datum).toString());
            ps.setDouble(2, diff);
            ps.setString(3, (titel == null || titel.isBlank()) ? "Manueller Saldoabgleich" : titel);
            ps.setString(4, "AUSGLEICH");
            ps.setInt(5, kontoId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Inserção manual para Bankkonto (categoria MANUELLE TRANSAKTION). */
    public void insertManuelleTransaktionFuerKonto(int nutzerId, int kontoId, LocalDate datum, double betrag, String titel) {
        final String sql = "INSERT INTO Transaktion (datum, betrag, titel, kategorie, konto_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (datum == null ? LocalDate.now() : datum).toString());
            ps.setDouble(2, betrag);
            ps.setString(3, (titel == null ? "" : titel.trim()));
            ps.setString(4, "MANUELLE TRANSAKTION");
            ps.setInt(5, kontoId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Inserção em Cash (categoria derivada no SELECT: AUSGLEICH se título for "Manueller Saldoabgleich"). */
    public void insertCashTransaktion(int nutzerId, LocalDate datum, double betrag, String titel) {
        final String sql = "INSERT INTO CashTransaktion (nutzer_id, datum, betrag, beschreibung) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            ps.setString(2, (datum == null ? LocalDate.now() : datum).toString());
            ps.setDouble(3, betrag);
            ps.setString(4, (titel == null ? "" : titel.trim())); // p.ex. "Manueller Saldoabgleich"
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Mapper
    // ───────────────────────────────────────────────────────────────────────────

    private Transaktion mapRow(ResultSet rs) throws SQLException {
        Transaktion t = new Transaktion();
        t.setTransaktionId(rs.getInt("id"));
        String ds = rs.getString("datum");
        if (ds != null && ds.length() >= 10) t.setDatum(LocalDate.parse(ds.substring(0, 10)));
        t.setBetrag(rs.getDouble("betrag"));
        t.setBeschreibung(rs.getString("beschreibung"));
        t.setKategorie(rs.getString("kategorie"));
        t.setQuelle(rs.getString("quelle"));
        if (rs.getObject("konto_id") != null) {
            Bankkonto k = new Bankkonto();
            k.setBankKontoId(rs.getInt("konto_id"));
            t.setKonto(k);
        }
        return t;
    }
}
