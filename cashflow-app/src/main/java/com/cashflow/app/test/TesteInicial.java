/*package com.cashflow.app.test;

import com.cashflow.app.dao.*;
import com.cashflow.app.model.*;
import com.cashflow.app.service.*;

import java.sql.SQLException;
import java.time.LocalDate;


public class TesteInicial {
    public static void main(String[] args) {
        try {
            // === 1. Criação de dois usuários ===
            Nutzer joao = new Nutzer(0, "João", "Silva", "joao@example.com", "senha123");
            Nutzer anna = new Nutzer(0, "Anna", "Schmidt", "anna@example.com", "senha123");

            NutzerDAO nutzerDAO = new NutzerDAO();
            nutzerDAO.save(joao);
            nutzerDAO.save(anna);
            System.out.println("✔ Nutzer João ID: " + joao.getNutzerId());
            System.out.println("✔ Nutzer Anna ID: " + anna.getNutzerId());

            // === 2. Criar Haushaltsgruppe e adicionar ambos ===
            Haushaltsgruppe gruppe = new Haushaltsgruppe();
            gruppe.setName("Família Silva-Schmidt");

            HaushaltsgruppeDAO gruppeDAO = new HaushaltsgruppeDAO();
            gruppeDAO.save(gruppe);

            NutzerHaushaltsgruppeDAO joinDAO = new NutzerHaushaltsgruppeDAO();
            joinDAO.nutzerZurGruppeHinzufuegen(joao.getNutzerId(), gruppe.getHaushaltsgruppeId());
            joinDAO.nutzerZurGruppeHinzufuegen(anna.getNutzerId(), gruppe.getHaushaltsgruppeId());
            System.out.println("✔ Grupo criado e usuários associados.");

            // === 3. Criar contas bancárias ===
            Bankkonto kontoJoao = new Bankkonto(0, "Deutsche Bank", "DE9912345678", "12345678", "DEUTDEFF", 1200.00, "EUR", joao);
            Bankkonto kontoAnna = new Bankkonto(0, "Sparkasse", "DE0023456789", "98765432", "SPKDEFFXXX", 2000.00, "EUR", anna);

            BankkontoDAO kontoDAO = new BankkontoDAO();
            kontoDAO.save(kontoJoao);
            kontoDAO.save(kontoAnna);
            System.out.println("✔ Contas bancárias criadas.");

            // === 4. Criar transações para cada conta ===
            TransaktionDAO transaktionDAO = new TransaktionDAO();

            transaktionDAO.save(new Transaktion(0, LocalDate.now().minusDays(5), -800, "Miete", "Wohnung", kontoJoao));
            transaktionDAO.save(new Transaktion(0, LocalDate.now().minusDays(3), -120, "Supermarkt", "Lebensmittel", kontoJoao));
            transaktionDAO.save(new Transaktion(0, LocalDate.now().minusDays(1), -50, "Internet", "Fixkosten", kontoJoao));

            transaktionDAO.save(new Transaktion(0, LocalDate.now().minusDays(4), -850, "Miete", "Wohnung", kontoAnna));
            transaktionDAO.save(new Transaktion(0, LocalDate.now().minusDays(2), -200, "Einkauf DM", "Haushalt", kontoAnna));
            transaktionDAO.save(new Transaktion(0, LocalDate.now().minusDays(1), -90, "Apotheke", "Gesundheit", kontoAnna));

            System.out.println("✔ Transações registradas.");

            // === 5. Consultar saldo individual ===
            double saldoJoao = kontoJoao.getAktuellerSaldo();
            double saldoAnna = kontoAnna.getAktuellerSaldo();
            double saldoTotal = saldoJoao + saldoAnna;

            System.out.println("💰 Saldo de João: " + saldoJoao + " EUR");
            System.out.println("💰 Saldo de Anna: " + saldoAnna + " EUR");
            System.out.println("💰💰 Saldo total do grupo: " + saldoTotal + " EUR");

            // === 6. Criar Sparziel para João ===
            Sparziel ziel = new Sparziel(0, "Viagem à Itália", 1500.00, LocalDate.now().plusMonths(5), "Férias", joao);
            SparzielDAO zielDAO = new SparzielDAO();
            zielDAO.save(ziel);

            SparzielService sparzielService = new SparzielService();
            sparzielService.zeigeStatus(ziel);

            // Simular recomendação
            double empfehlung = sparzielService.berechneMonatlicheEmpfehlung(ziel);
            System.out.printf("📈 Recomendação mensal: %.2f EUR%n", empfehlung);

            // Simular contribuição para poupança
            Transaktion beitrag = sparzielService.simuliereBeitrag(ziel, kontoJoao);
            transaktionDAO.save(beitrag);

            System.out.println("✔ Contribuição simulada registrada.");
            sparzielService.zeigeStatus(ziel);

            // === 7. Usar PrognoseService ===
            PrognoseService prognoseService = new PrognoseService();
            double prognose = prognoseService.berechnePrognostizierterSaldo(kontoJoao, LocalDate.now().withDayOfMonth(28));
            System.out.printf("🔮 Saldo projetado no fim do mês (João): %.2f EUR%n", prognose);

        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("❌ Erro durante execução do teste.");
        }
    }
}*/
