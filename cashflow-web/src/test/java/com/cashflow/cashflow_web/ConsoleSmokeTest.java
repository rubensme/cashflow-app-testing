package com.cashflow.cashflow_web;

import com.cashflow.app.dao.*;
import com.cashflow.app.model.*;
import com.cashflow.app.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ConsoleSmokeTest {

    public static void main(String[] args) {
        NutzerDAO nutzerDAO = new NutzerDAO();
        BankkontoDAO bankkontoDAO = new BankkontoDAO();
        TransaktionDAO transaktionDAO = new TransaktionDAO();
        CashTransaktionDAO cashDAO = new CashTransaktionDAO();

        String email = "console@test.local";

        try {
            // 0) Limpar dados antigos do mesmo e-mail (idempotente pro teste)
            Nutzer existente = nutzerDAO.findByEmail(email);
            if (existente != null) {
                try (Connection c = DatabaseManager.getConnection()) {
                    // Apaga em ordem de FK
                    try (PreparedStatement ps1 = c.prepareStatement("DELETE FROM Transaktion WHERE konto_id IN (SELECT konto_id FROM Bankkonto WHERE nutzer_id = ?)")) {
                        ps1.setInt(1, existente.getNutzerId());
                        ps1.executeUpdate();
                    }
                    try (PreparedStatement ps2 = c.prepareStatement("DELETE FROM CashTransaktion WHERE nutzer_id = ?")) {
                        ps2.setInt(1, existente.getNutzerId());
                        ps2.executeUpdate();
                    }
                    try (PreparedStatement ps3 = c.prepareStatement("DELETE FROM Bankkonto WHERE nutzer_id = ?")) {
                        ps3.setInt(1, existente.getNutzerId());
                        ps3.executeUpdate();
                    }
                    nutzerDAO.deleteById(existente.getNutzerId());
                }
            }

            // 1) Criar usuário
            Nutzer n = new Nutzer();
            n.setVorname("Max");
            n.setNachname("Muster");
            n.setEmail(email);
            n.setPasswort("123456");
            nutzerDAO.save(n);
            System.out.println("Nutzer criado: id=" + n.getNutzerId());

            // 2) Criar 2 contas bancárias com saldos atuais
            Bankkonto k1 = new Bankkonto();
            k1.setNutzer(n);
            k1.setNameDerBank("Commerzbank");
            k1.setIban("DE11 1111 1111 1111 1111 11");
            k1.setBic("COBADEFFXXX");
            k1.setAktuellerSaldo(1500.00);
            bankkontoDAO.save(k1);

            Bankkonto k2 = new Bankkonto();
            k2.setNutzer(n);
            k2.setNameDerBank("Sparkasse");
            k2.setIban("DE22 2222 2222 2222 2222 22");
            k2.setBic("SPKDEFFXXX");
            k2.setAktuellerSaldo(800.00);
            bankkontoDAO.save(k2);

            System.out.println("Konten criadas: " + k1.getBankKontoId() + ", " + k2.getBankKontoId());

            // 3) Inserir algumas transações bancárias (só pra popular “últimas 5”)
            Transaktion t1 = new Transaktion();
            t1.setDatum(LocalDate.now());
            t1.setBetrag(-50.00);
            t1.setBeschreibung("Supermarkt");
            t1.setKategorie("Einkauf");
            t1.setKonto(k1);
            transaktionDAO.save(t1);

            Transaktion t2 = new Transaktion();
            t2.setDatum(LocalDate.now().minusDays(1));
            t2.setBetrag(-20.00);
            t2.setBeschreibung("Bahn Ticket");
            t2.setKategorie("Transport");
            t2.setKonto(k2);
            transaktionDAO.save(t2);

            Transaktion t3 = new Transaktion();
            t3.setDatum(LocalDate.now().minusDays(2));
            t3.setBetrag(1200.00);
            t3.setBeschreibung("Gehalt");
            t3.setKategorie("Einnahmen");
            t3.setKonto(k1);
            transaktionDAO.save(t3);

            // 4) Inserir algumas transações CASH
            CashTransaktion c1 = new CashTransaktion();
            c1.setDatum(LocalDate.now().toString());
            c1.setBeschreibung("Bar-Auszahlung");
            c1.setBetrag(-30.00);
            c1.setNutzerId(n.getNutzerId());
            cashDAO.save(c1);

            CashTransaktion c2 = new CashTransaktion();
            c2.setDatum(LocalDate.now().minusDays(1).toString());
            c2.setBeschreibung("Kaffee");
            c2.setBetrag(-3.50);
            c2.setNutzerId(n.getNutzerId());
            cashDAO.save(c2);

            CashTransaktion c3 = new CashTransaktion();
            c3.setDatum(LocalDate.now().minusDays(3).toString());
            c3.setBeschreibung("Bar-Einnahme");
            c3.setBetrag(40.00);
            c3.setNutzerId(n.getNutzerId());
            cashDAO.save(c3);

            // 5) Calcular saldos
            double saldoBank = bankkontoDAO.findByNutzerId(n.getNutzerId())
                    .stream()
                    .mapToDouble(Bankkonto::getAktuellerSaldo)
                    .sum();

            double saldoCash = cashDAO.sumCashSaldoByNutzerId(n.getNutzerId());
            double gesamtsaldo = saldoBank + saldoCash;

            System.out.println("Saldo contas bancárias  : " + String.format("%.2f", saldoBank) + " €");
            System.out.println("Saldo CASH              : " + String.format("%.2f", saldoCash) + " €");
            System.out.println("GESAMTSALDO             : " + String.format("%.2f", gesamtsaldo) + " €");

            // 6) Últimas 5 transações (bancárias) do usuário
            List<Transaktion> last5 = transaktionDAO.findLast5ByNutzerIdOrderByDatumDesc(n.getNutzerId());
            System.out.println("\nLetzte Transaktionen:");
            for (Transaktion t : last5) {
                System.out.println(" - " + t.getDatum() + " | " + t.getBeschreibung() + " | " + t.getKategorie() + " | " + t.getBetrag());
            }

            System.out.println("\nOK: Smoke test finalizado.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
