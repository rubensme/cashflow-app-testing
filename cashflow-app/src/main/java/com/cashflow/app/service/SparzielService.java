package com.cashflow.app.service;

import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Sparziel;
import com.cashflow.app.model.Transaktion;
import java.util.List;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SparzielService {

    private static final AtomicInteger transaktionsZaehler = new AtomicInteger(1000);

    /**
     * Berechnet den empfohlenen monatlichen Sparbetrag, um das Ziel bis zum Enddatum zu erreichen.
     */
    public double berechneMonatlicheEmpfehlung(Sparziel ziel) {
        long verbleibendeMonate = ChronoUnit.MONTHS.between(
            LocalDate.now().withDayOfMonth(1),
            ziel.getFaelligBis().withDayOfMonth(1)
        );

        verbleibendeMonate = Math.max(verbleibendeMonate, 1);

        return ziel.getRestbetrag() / verbleibendeMonate;
    }

    /**
     * Simuliert eine Überweisung des empfohlenen Betrags für das Sparziel.
     */
    public Transaktion simuliereBeitrag(Sparziel ziel, Bankkonto konto) {
        double empfehlung = berechneMonatlicheEmpfehlung(ziel);
        return erstelleBeitrag(ziel, konto, empfehlung);
    }

    /**
     * Erstellt eine Transaktion mit einem benutzerdefinierten Beitrag für das Sparziel.
     */
    public Transaktion erstelleBeitrag(Sparziel ziel, Bankkonto konto, double betrag) {
        if (betrag <= 0 || betrag > konto.getAktuellerSaldo()) {
            throw new IllegalArgumentException("Ungültiger Beitrag: " + betrag);
        }

        Transaktion t = new Transaktion(
            transaktionsZaehler.getAndIncrement(),
            LocalDate.now(),
            -betrag,
            "Überweisung für Sparziel: " + ziel.getTitel(),
            "Sparziel",
            konto
        );

        t.setZugeordneteAusgabeId(ziel.getSparzielId());
        konto.transaktionHinzufuegen(t);
        ziel.beitragHinzufuegen(betrag);

        return t;
    }

    /**
     * Gibt den aktuellen Status des Sparziels aus.
     */
    public void zeigeStatus(Sparziel ziel) {
        System.out.println("🎯 Sparziel: " + ziel.getTitel());
        System.out.println("Zielbetrag: " + ziel.getZielBetrag() + " EUR");
        System.out.println("Gespart: " + ziel.getAktuellerBetrag() + " EUR");
        System.out.println("Fehlend: " + ziel.getRestbetrag() + " EUR");
        System.out.println("Enddatum: " + ziel.getFaelligBis());
        System.out.println();
    }
    
    public boolean analyseTransaktionenUndVorschlagen(Bankkonto konto, Sparziel ziel) {
        List<Transaktion> transaktionen = konto.getTransaktionen();

        for (Transaktion t : transaktionen) {
            String beschreibung = t.getBeschreibung().toLowerCase();
            boolean istMoeglicheEinzahlung = 
                    (beschreibung.contains("spar") || beschreibung.contains("ziel") || beschreibung.contains("unterkonto"))
                    && t.getBetrag() < 0;

            if (istMoeglicheEinzahlung) {
                System.out.println("⚠️ Mögliche Einzahlung gefunden:");
                System.out.println(" → " + t.getBeschreibung() + " | Betrag: " + t.getBetrag() + " EUR");

                // Nutzer kann manuell entscheiden, ob diese Transaktion gezählt wird
                return true;
            }
        }

        return false;
    }
}
