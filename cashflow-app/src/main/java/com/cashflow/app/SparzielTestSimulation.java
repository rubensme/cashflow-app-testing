package com.cashflow.app;

import com.cashflow.app.model.*;
import com.cashflow.app.service.SparzielService;

import java.time.LocalDate;

public class SparzielTestSimulation {
    public static void main(String[] args) {
        // ✅ Criar usuário e conta
        Nutzer max = new Nutzer(1, "Max", "Muster", "max@example.com", "pass");
        Bankkonto konto = new Bankkonto(1, "Sparkasse", "DE123456", "123456", "BYLADEM1", 2000.0, "EUR", max);
        max.kontoHinzufuegen(konto);

        // ✅ Criar meta de poupança: 500€ até o fim do mês
        Sparziel ziel = new Sparziel(1, "Reise nach Italien", 500.0, LocalDate.now().withDayOfMonth(30), "Sommerurlaub", max);
        max.sparzielHinzufuegen(ziel);

        // ✅ Criar transações (somente uma é poupança)
        konto.transaktionHinzufuegen(new Transaktion(1, LocalDate.now(), -100.0, "Supermarkt", "Rewe", konto));
        konto.transaktionHinzufuegen(new Transaktion(2, LocalDate.now(), -150.0, "Transport", "Bahn", konto));
        konto.transaktionHinzufuegen(new Transaktion(3, LocalDate.now(), -200.0, "Überweisung für Sparziel", "Unterkonto Sparen", konto));

        // ✅ Serviço para análise do Sparziel
        SparzielService service = new SparzielService();

        // 🔍 Mostrar progresso inicial (antes de confirmar depósito)
        System.out.println("🎯 Ziel: " + ziel.getTitel());
        System.out.println("   Gespart: " + ziel.getAktuellerBetrag() + " EUR");
        System.out.println("   Prozent: " + ziel.getProzentualerFortschritt() + "%");

        // ✅ App verifica se há transações compatíveis
        boolean erkannt = service.analyseTransaktionenUndVorschlagen(konto, ziel);

        if (erkannt) {
            System.out.println("⚠️  Hinweis: Es gibt eine Transaktion, die wie eine Einzahlung für das Sparziel aussieht.");
            // ✅ Simular confirmação do usuário
            System.out.println("👉 Benutzer bestätigt manuell die Zuordnung.");
            ziel.beitragHinzufuegen(200.0);
        }

        // ✅ Mostrar progresso após confirmação
        System.out.println("✅ Aktualisierter Sparstand:");
        System.out.println("   Gespart: " + ziel.getAktuellerBetrag() + " EUR");
        System.out.println("   Prozent: " + ziel.getProzentualerFortschritt() + "%");

        if (!ziel.istErreicht()) {
            System.out.println("💡 Noch fehlen: " + ziel.getRestbetrag() + " EUR bis zum Ziel.");
        } else {
            System.out.println("🎉 Sparziel erreicht!");
        }
    }
}
