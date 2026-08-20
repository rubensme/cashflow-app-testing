package com.cashflow.app;

import java.time.LocalDate;
import java.util.List;

import com.cashflow.app.model.*;
import com.cashflow.app.model.FesteAusgabe.Rhythmus;
import com.cashflow.app.service.SparzielService;
import com.cashflow.app.service.AuthService;

public class Main {
    public static void main(String[] args) {

        // ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
        // ▓▓ TESTE 0: LOGIN E CADASTRO       ▓▓
        // ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

        AuthService authService = new AuthService();

        boolean registriert = authService.registrieren("max@example.com", "geheim", "Max", "Mustermann");
        System.out.println("Registrado com sucesso? " + (registriert ? "Sim" : "Não"));

        Nutzer eingeloggterNutzer = authService.login("max@example.com", "geheim");
        if (eingeloggterNutzer != null) {
            System.out.println("Login bem-sucedido!");
            System.out.println("Bem-vindo, " + eingeloggterNutzer.getVorname() + " " + eingeloggterNutzer.getNachname());
        } else {
            System.out.println("Login falhou.");
        }

        Nutzer falsch = authService.login("max@example.com", "senhaErrada");
        System.out.println("Login com senha errada: " + (falsch == null ? "bloqueado (ok)" : "ERRO: aceitou"));

        boolean geloescht = authService.kontoLoeschen("max@example.com", "geheim");
        System.out.println("Conta deletada? " + (geloescht ? "Sim" : "Não"));

        Nutzer nachLoeschung = authService.login("max@example.com", "geheim");
        System.out.println("Login após exclusão: " + (nachLoeschung == null ? "bloqueado (ok)" : "ERRO: ainda existe"));



        // ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
        // ▓▓ TESTE 1: KONTO UND FIXKOSTEN   ▓▓
        // ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

        Nutzer nutzer = new Nutzer(1, "Max", "Mustermann", "max@example.com", "geheim");

        Bankkonto konto = new Bankkonto(1, "Sparkasse", "DE0012345678", "12345678", "BYLADEM1", 1000.00, "EUR", nutzer);
        nutzer.kontoHinzufuegen(konto);

        FesteAusgabe miete = new FesteAusgabe(1, "Miete", 100.0, LocalDate.now(), Rhythmus.MONATLICH);
        nutzer.festeAusgabeHinzufuegen(miete);

        Transaktion mieteBezahlt = new Transaktion(1, LocalDate.now(), -100.0, "Miete", "Wohnung", konto);
        konto.transaktionHinzufuegen(mieteBezahlt);

        System.out.println("\n🟢 Aktueller Saldo: " + konto.getAktuellerSaldo() + " EUR");

        LocalDate endeDesMonats = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        double prognoseSaldo = konto.getPrognostizierterSaldoBis(endeDesMonats);
        System.out.println("📅 Prognostizierter Saldo bis Monatsende: " + prognoseSaldo + " EUR");

        List<FesteAusgabe> offeneAusgaben = konto.getOffeneAusgabenBis(endeDesMonats);
        System.out.println("🔴 Noch nicht erkannte Ausgaben:");
        for (FesteAusgabe fa : offeneAusgaben) {
            System.out.println("- " + fa.getTitel() + " (" + fa.getNaechsteFaelligkeit() + "): " + fa.getBetrag() + " EUR");
        }


        // ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
        // ▓▓ TESTE 2: SPARZIEL UND ANALYSE  ▓▓
        // ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

        Sparziel sparziel = new Sparziel(1, "Reise nach Italien", 500.0, LocalDate.now().plusMonths(2), "Sommerurlaub", nutzer);
        sparziel.betragHinzufuegen(100.0);

        Transaktion sparEinzahlung = new Transaktion(2, LocalDate.now(), -100.0, "Sparziel Italien", "Sparkonto", konto);
        konto.transaktionHinzufuegen(sparEinzahlung);

        SparzielService sparzielService = new SparzielService();
        boolean erkannt = sparzielService.analyseTransaktionenUndVorschlagen(konto, sparziel);

        System.out.println("\n🎯 Sparziel-Analyse:");
        System.out.println("  ➤ Ziel: " + sparziel.getTitel());
        System.out.println("  ➤ Zielbetrag: " + sparziel.getZielBetrag() + " EUR");
        System.out.println("  ➤ Bereits gespart: " + sparziel.getAktuellerBetrag() + " EUR");
        System.out.println("  ➤ Fehlender Betrag: " + sparziel.getRestbetrag() + " EUR");
        System.out.println("  ➤ Passende Transaktion erkannt? " + (erkannt ? "✅ Ja" : "❌ Nein"));
    }
}
