package com.cashflow.app.dao;

import com.cashflow.app.database.DatabaseManager;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Nutzer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BankkontoDAO {

    // Inserir novo Bankkonto
    // Observação: não setamos is_checked explicitamente, deixamos o DEFAULT=1 do banco.
    public void save(Bankkonto konto) throws SQLException {
        String sql = "INSERT INTO bankkonto (nutzer_id, bankname, aktueller_saldo, iban, bic) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, konto.getNutzer().getNutzerId());
            stmt.setString(2, konto.getNameDerBank());     // bankname
            stmt.setDouble(3, konto.getAktuellerSaldo());  // aktueller_saldo
            stmt.setString(4, konto.getIban());
            stmt.setString(5, konto.getBic());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new SQLException("Fehler beim Einfügen des Kontos.");

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    konto.setBankKontoId(keys.getInt(1));  // konto_id
                }
            }
        }
    }

    // Buscar todos os konten de um usuário (inclui is_checked)
    public List<Bankkonto> findByNutzerId(int nutzerId) throws SQLException {
        String sql = "SELECT konto_id, nutzer_id, bankname, aktueller_saldo, iban, bic, " +
                     "       COALESCE(is_checked, 1) AS is_checked " +
                     "FROM bankkonto WHERE nutzer_id = ?";
        List<Bankkonto> konten = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nutzerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Bankkonto konto = new Bankkonto();
                konto.setBankKontoId(rs.getInt("konto_id"));
                konto.setNameDerBank(rs.getString("bankname"));
                konto.setAktuellerSaldo(rs.getDouble("aktueller_saldo"));
                konto.setIban(rs.getString("iban"));
                konto.setBic(rs.getString("bic"));

                Nutzer nutzer = new Nutzer();
                nutzer.setNutzerId(rs.getInt("nutzer_id"));
                konto.setNutzer(nutzer);

                // novo campo persistido
                konto.setChecked(rs.getInt("is_checked") == 1);

                konten.add(konto);
            }
        }
        return konten;
    }

    // ✅ Buscar um Bankkonto por ID (sem lançar SQLException)
    public Bankkonto findById(int kontoId) {
        String sql = "SELECT konto_id, nutzer_id, bankname, aktueller_saldo, iban, bic, " +
                     "       COALESCE(is_checked, 1) AS is_checked " +
                     "FROM bankkonto WHERE konto_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kontoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Bankkonto k = new Bankkonto();
                    k.setBankKontoId(rs.getInt("konto_id"));
                    k.setNameDerBank(rs.getString("bankname"));
                    k.setAktuellerSaldo(rs.getDouble("aktueller_saldo"));
                    k.setIban(rs.getString("iban"));
                    k.setBic(rs.getString("bic"));

                    Nutzer n = new Nutzer();
                    n.setNutzerId(rs.getInt("nutzer_id"));
                    k.setNutzer(n);

                    k.setChecked(rs.getInt("is_checked") == 1);
                    return k;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Deletar konto por ID
    public void deleteById(int kontoId) throws SQLException {
        String sql = "DELETE FROM bankkonto WHERE konto_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, kontoId);
            stmt.executeUpdate();
        }
    }

    // Atualizar saldo (lança SQLException)
    public void updateSaldo(int kontoId, double neuerSaldo) throws SQLException {
        String sql = "UPDATE bankkonto SET aktueller_saldo = ? WHERE konto_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, neuerSaldo);
            stmt.setInt(2, kontoId);
            stmt.executeUpdate();
        }
    }

    // ✅ Variante "quiet": atualiza saldo sem propagar SQLException
    public boolean updateSaldoSafe(int kontoId, double neuerSaldo) {
        try {
            updateSaldo(kontoId, neuerSaldo);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Soma dos saldos por usuário (todas as contas)
    public double sumSaldoByNutzerId(int nutzerId) {
        String sql = "SELECT COALESCE(SUM(aktueller_saldo), 0) AS total " +
                     "FROM bankkonto WHERE nutzer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            // log conforme seu padrão
            e.printStackTrace();
        }
        return 0.0;
    }
    
 // Atualiza dados básicos: bankname, IBAN, BIC
    public void updateStammdaten(int kontoId, String bankname, String iban, String bic) throws SQLException {
        final String sql = "UPDATE bankkonto SET bankname = ?, iban = ?, bic = ? WHERE konto_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bankname);
            ps.setString(2, iban);
            ps.setString(3, bic);
            ps.setInt(4, kontoId);
            ps.executeUpdate();
        }
    }


    // ✅ Soma dos saldos apenas das contas marcadas (is_checked = 1)
    public double sumCheckedSaldoByNutzerId(int nutzerId) {
        String sql = "SELECT COALESCE(SUM(aktueller_saldo), 0) AS total " +
                     "FROM bankkonto WHERE nutzer_id = ? AND COALESCE(is_checked,1) = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // ✅ Atualiza o is_checked de um único konto
    public void updateChecked(int kontoId, boolean checked) throws SQLException {
        String sql = "UPDATE bankkonto SET is_checked = ? WHERE konto_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, checked ? 1 : 0);
            stmt.setInt(2, kontoId);
            stmt.executeUpdate();
        }
    }

    // ✅ Atualiza em lote: zera tudo e marca apenas os IDs fornecidos
    public void updateCheckedByIds(int nutzerId, Set<Integer> checkedIds) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Desmarcar todas as contas do usuário
                String resetSql = "UPDATE bankkonto SET is_checked = 0 WHERE nutzer_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(resetSql)) {
                    ps.setInt(1, nutzerId);
                    ps.executeUpdate();
                }

                // 2) Marcar as selecionadas
                if (checkedIds != null && !checkedIds.isEmpty()) {
                    StringBuilder in = new StringBuilder();
                    for (int i = 0; i < checkedIds.size(); i++) {
                        if (i > 0) in.append(",");
                        in.append("?");
                    }
                    String setSql = "UPDATE bankkonto SET is_checked = 1 " +
                                    "WHERE nutzer_id = ? AND konto_id IN (" + in + ")";
                    try (PreparedStatement ps = conn.prepareStatement(setSql)) {
                        int idx = 1;
                        ps.setInt(idx++, nutzerId);
                        for (Integer id : checkedIds) {
                            ps.setInt(idx++, id);
                        }
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ✅ Lista apenas os IDs das contas marcadas do usuário
    public Set<Integer> findCheckedKontoIdsByNutzerId(int nutzerId) {
        String sql = "SELECT konto_id FROM bankkonto WHERE nutzer_id = ? AND COALESCE(is_checked,1) = 1";
        Set<Integer> ids = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutzerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("konto_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
}
