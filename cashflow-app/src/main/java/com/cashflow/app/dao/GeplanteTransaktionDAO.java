package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.dto.GeplanteOccurrence;
import com.cashflow.app.model.GeplanteTransaktion;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DAO für geplante (wiederkehrende oder einmalige) Transaktionen.
 * Enthält CRUD-Basismethoden und eine Projektion der anfallenden Vorkommnisse
 * bis zu einem Zieldatum (für die Finanzprognose).
 */
public class GeplanteTransaktionDAO {

    // ===== CREATE =====
    public void save(GeplanteTransaktion g) throws SQLException {
        final String sql =
                "INSERT INTO GeplanteTransaktion (nutzer_id, titel, start_datum, betrag, periode, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Normalizações e defaults seguros
            int nutzerId = g.getNutzerId();
            String titel = safeString(g.getTitel());
            LocalDate start = (g.getStartDatum() == null) ? LocalDate.now() : g.getStartDatum();
            double betrag = g.getBetrag();
            String periode = normalizePeriode(g.getPeriode()); // garante MONATLICH/EINMALIG/etc.
            String status  = normalizeStatus(g.getStatus());   // AKTIV/BEZAHLT

            ps.setInt(1, nutzerId);
            ps.setString(2, titel);
            ps.setString(3, start.toString()); // yyyy-MM-dd
            ps.setDouble(4, betrag);
            ps.setString(5, periode);
            ps.setString(6, status);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) g.setGeplanteId(rs.getInt(1));
            }
        }
    }

    // ===== READ: por ID (útil para edição) =====
    public GeplanteTransaktion findById(int geplanteId) {
        final String sql =
                "SELECT geplante_id, nutzer_id, titel, start_datum, betrag, periode, status " +
                "FROM GeplanteTransaktion WHERE geplante_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, geplanteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GeplanteTransaktion g = new GeplanteTransaktion();
                    g.setGeplanteId(rs.getInt("geplante_id"));
                    g.setNutzerId(rs.getInt("nutzer_id"));
                    g.setTitel(safeString(rs.getString("titel")));

                    String ds = rs.getString("start_datum");
                    if (ds != null && ds.length() >= 10) {
                        g.setStartDatum(LocalDate.parse(ds.substring(0, 10)));
                    }

                    g.setBetrag(rs.getDouble("betrag"));
                    g.setPeriode(safeString(rs.getString("periode")));
                    g.setStatus(safeString(rs.getString("status")));
                    return g;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===== READ (Lista por Nutzer) =====
    public List<GeplanteTransaktion> findByNutzerId(int nutzerId) {
        final String sql =
                "SELECT geplante_id, nutzer_id, titel, start_datum, betrag, periode, status " +
                "FROM GeplanteTransaktion WHERE nutzer_id = ?";
        List<GeplanteTransaktion> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, nutzerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GeplanteTransaktion g = new GeplanteTransaktion();
                    g.setGeplanteId(rs.getInt("geplante_id"));
                    g.setNutzerId(rs.getInt("nutzer_id"));
                    g.setTitel(safeString(rs.getString("titel")));

                    String ds = rs.getString("start_datum");
                    if (ds != null && ds.length() >= 10) {
                        g.setStartDatum(LocalDate.parse(ds.substring(0, 10))); // nur Datumsteil
                    }

                    g.setBetrag(rs.getDouble("betrag"));
                    g.setPeriode(safeString(rs.getString("periode")));
                    g.setStatus(safeString(rs.getString("status")));
                    list.add(g);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===== UPDATE =====
    public void update(GeplanteTransaktion g) {
        final String sql =
                "UPDATE GeplanteTransaktion " +
                "SET titel = ?, start_datum = ?, betrag = ?, periode = ?, status = ? " +
                "WHERE geplante_id = ? AND nutzer_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String titel   = safeString(g.getTitel());
            LocalDate start = (g.getStartDatum() == null) ? LocalDate.now() : g.getStartDatum();
            String periode = normalizePeriode(g.getPeriode()); // <— normaliza ANTES de persistir
            String status  = normalizeStatus(g.getStatus());

            ps.setString(1, titel);
            ps.setString(2, start.toString()); // yyyy-MM-dd
            ps.setDouble(3, g.getBetrag());
            ps.setString(4, periode);
            ps.setString(5, status);
            ps.setInt(6, g.getGeplanteId());
            ps.setInt(7, g.getNutzerId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===== DELETE =====
    public void deleteById(int geplanteId) {
        final String sql = "DELETE FROM GeplanteTransaktion WHERE geplante_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, geplanteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========================================================================
    // ===== NOVO: READ por múltiplos usuários (somente "ativas")          =====
    // ========================================================================

    /**
     * Retorna todas as planejadas "ativas" (tratamos AKTIV e OFFEN como ativas)
     * para uma coleção de nutzer_id. Útil para páginas de grupo.
     */
    public List<GeplanteTransaktion> findAktivByNutzerIds(Collection<Integer> nutzerIds) throws SQLException {
        List<GeplanteTransaktion> out = new ArrayList<>();
        if (nutzerIds == null || nutzerIds.isEmpty()) return out;

        final String placeholders = nutzerIds.stream().map(x -> "?").collect(Collectors.joining(","));
        final String sql =
                "SELECT geplante_id, nutzer_id, titel, start_datum, betrag, periode, status " +
                "FROM GeplanteTransaktion " +
                "WHERE (status IN ('AKTIV','OFFEN')) AND nutzer_id IN (" + placeholders + ")";

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int i = 1;
            for (Integer id : nutzerIds) ps.setInt(i++, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GeplanteTransaktion g = new GeplanteTransaktion();
                    g.setGeplanteId(rs.getInt("geplante_id"));
                    g.setNutzerId(rs.getInt("nutzer_id"));
                    g.setTitel(safeString(rs.getString("titel")));

                    String ds = rs.getString("start_datum");
                    if (ds != null && ds.length() >= 10) {
                        g.setStartDatum(LocalDate.parse(ds.substring(0, 10)));
                    }

                    g.setBetrag(rs.getDouble("betrag"));
                    g.setPeriode(safeString(rs.getString("periode")));
                    g.setStatus(safeString(rs.getString("status")));
                    out.add(g);
                }
            }
        }
        return out;
    }

    /**
     * Projeta ocorrências por membro no intervalo [fromDate, targetDate] (ambos inclusivos).
     * Retorna um mapa nutzerId -> lista de ocorrências.
     * O tratamento de status segue findOccurrencesUpTo (BEZAHLT suprime apenas a ocorrência
     * exata em start_datum; recorrências futuras permanecem).
     */
    public Map<Integer, List<GeplanteOccurrence>> findOccurrencesUpToByNutzerIds(
            Collection<Integer> nutzerIds, LocalDate fromDate, LocalDate targetDate) throws SQLException {

        Map<Integer, List<GeplanteOccurrence>> result = new HashMap<>();
        if (nutzerIds == null || nutzerIds.isEmpty()) return result;

        List<GeplanteTransaktion> basis = findAktivByNutzerIds(nutzerIds);

        for (GeplanteTransaktion g : basis) {
            final int nid = g.getNutzerId();
            result.computeIfAbsent(nid, k -> new ArrayList<>());

            LocalDate start = (g.getStartDatum() == null) ? LocalDate.now() : g.getStartDatum();
            String periode  = normalizePeriode(g.getPeriode());
            boolean skipStart = "BEZAHLT".equalsIgnoreCase(g.getStatus());

            LocalDate d = start;

            // Avança até entrar no intervalo
            while (d.isBefore(fromDate)) {
                d = next(d, periode);
            }

            while (!d.isAfter(targetDate)) {
                if (!(skipStart && d.equals(start))) {
                    result.get(nid).add(new GeplanteOccurrence(d, safeString(g.getTitel()), g.getBetrag()));
                }
                if ("EINMALIG".equalsIgnoreCase(periode)) break;
                d = next(d, periode);
            }
        }

        // Ordena cada lista por data
        for (List<GeplanteOccurrence> lst : result.values()) {
            lst.sort(Comparator.comparing(GeplanteOccurrence::getDatum));
        }
        return result;
    }

    // ========================================================================
    // ===== Projektion der Vorkommnisse bis Zieltermin (inkl.)            =====
    // ========================================================================

    /**
     * Erzeugt Vorkommnisse aller geplanten Transaktionen eines Nutzers im Intervall
     * [fromDate, targetDate] (beide inkl.).
     *
     * Regeln:
     * - Wenn status = BEZAHLT, wird die Vorkommnis exakt am start_datum unterdrückt.
     *   (Zukünftige Wiederholungen werden trotzdem berücksichtigt.)
     * - Beträge: Ausgaben negativ, Einnahmen positiv.
     */
    public List<GeplanteOccurrence> findOccurrencesUpTo(int nutzerId, LocalDate fromDate, LocalDate targetDate) {
        List<GeplanteOccurrence> occ = new ArrayList<>();
        List<GeplanteTransaktion> basis = findByNutzerId(nutzerId);

        for (GeplanteTransaktion g : basis) {
            LocalDate start = (g.getStartDatum() == null) ? LocalDate.now() : g.getStartDatum();
            String periode  = normalizePeriode(g.getPeriode()); // normalizar para o cálculo
            boolean skipStart = "BEZAHLT".equalsIgnoreCase(g.getStatus());

            LocalDate d = start;

            // bis in den betrachteten Zeitraum vorspulen
            while (d.isBefore(fromDate)) {
                d = next(d, periode);
            }

            // Vorkommnisse bis zum Zieltermin erzeugen
            while (!d.isAfter(targetDate)) {
                if (!(skipStart && d.equals(start))) {
                    occ.add(new GeplanteOccurrence(d, safeString(g.getTitel()), g.getBetrag()));
                }
                if ("EINMALIG".equalsIgnoreCase(periode)) break;
                d = next(d, periode);
            }
        }

        // nach Datum sortieren (aufsteigend)
        occ.sort((a, b) -> a.getDatum().compareTo(b.getDatum()));
        return occ;
    }

    // ===== Periode → nächste Fälligkeit =====
    private LocalDate next(LocalDate d, String periode) {
        if ("MONATLICH".equalsIgnoreCase(periode))        return d.plusMonths(1);
        if ("WOECHENTLICH".equalsIgnoreCase(periode))     return d.plusWeeks(1);
        if ("VIERTELJAEHRLICH".equalsIgnoreCase(periode)) return d.plusMonths(3);
        if ("JAEHRLICH".equalsIgnoreCase(periode))        return d.plusYears(1);
        // EINMALIG oder unbekannt: keine Wiederholung -> weit in die Zukunft schieben
        return d.plusYears(1000);
    }

    // ===== Helpers =====
    private static String safeString(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Normaliza entradas de período para o set suportado pela app/DB.
     * Aceita variações com minúsculas, trema, espaços, etc.
     * Default: MONATLICH quando nulo/vazio; EINMALIG quando não reconhecido.
     */
    private static String normalizePeriode(String p) {
        if (p == null || p.trim().isEmpty()) return "MONATLICH";
        String x = p.trim().toUpperCase();
        // mapear possíveis variações
        if (x.startsWith("EINM")) return "EINMALIG";
        if (x.startsWith("MONAT")) return "MONATLICH";
        if (x.startsWith("WOECH") || x.startsWith("WÖCH")) return "WOECHENTLICH";
        if (x.startsWith("VIERTEL")) return "VIERTELJAEHRLICH";
        if (x.startsWith("JAEHR") || x.startsWith("JÄHR")) return "JAEHRLICH";
        // se vier algo estranho, não falhar: persistir EINMALIG
        return "EINMALIG";
    }

    private static String normalizeStatus(String s) {
        if (s == null || s.trim().isEmpty()) return "AKTIV";
        String x = s.trim().toUpperCase();
        if (x.startsWith("AKT")) return "AKTIV";
        if (x.startsWith("BEZ")) return "BEZAHLT";
        // Tratar "OFFEN" como ativo no restante dos métodos, não na persistência
        if (x.startsWith("OFF")) return "AKTIV";
        return "AKTIV";
    }
}
