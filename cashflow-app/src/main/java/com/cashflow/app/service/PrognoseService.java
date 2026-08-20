package com.cashflow.app.service;

import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.FesteAusgabe;
import com.cashflow.app.model.Transaktion;
import java.time.temporal.ChronoUnit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service für Finanzprognosen eines Kontos.
 */
public class PrognoseService {

    /**
     * Gibt alle festen Ausgaben zurück, die bis zum angegebenen Datum fällig werden
     * und noch nicht als bezahlt erkannt wurden.
     */
    public List<FesteAusgabe> getOffeneAusgabenImZeitraum(Bankkonto konto, LocalDate bisDatum) {
        List<FesteAusgabe> offeneAusgaben = new ArrayList<>();

        for (FesteAusgabe ausgabe : konto.getNutzer().getFesteAusgaben()) {

            if (ausgabe.faelligImMonat(bisDatum) && !wurdeBereitsBezahlt(ausgabe, konto.getTransaktionen())) {
                offeneAusgaben.add(ausgabe);
            }
        }

        return offeneAusgaben;
    }

    /**
     * Berechnet den projizierten Kontostand zum Monatsende,
     * basierend auf dem aktuellen Saldo und geplanten Ausgaben.
     */
    public double berechnePrognostizierterSaldo(Bankkonto konto, LocalDate bisDatum) {
        double saldo = konto.getAktuellerSaldo();

        List<FesteAusgabe> offene = getOffeneAusgabenImZeitraum(konto, bisDatum);
        for (FesteAusgabe ausgabe : offene) {
            saldo -= ausgabe.getBetrag();
        }

        return saldo;
    }

    /**
     * Prüft, ob eine feste Ausgabe durch eine reale Transaktion bereits ausgeführt wurde.
     */
    private boolean wurdeBereitsBezahlt(FesteAusgabe ausgabe, List<Transaktion> transaktionen) {
        for (Transaktion t : transaktionen) {
            boolean betragGleich =
                    t.getBetrag() < 0
                            && Math.abs(Math.abs(t.getBetrag()) - ausgabe.getBetrag()) < 0.01;
            boolean datumNah =
                    Math.abs(ChronoUnit.DAYS.between(
                            t.getDatum(),
                            ausgabe.getNaechsteFaelligkeit()
                    )) <= 3;

            if (betragGleich && datumNah) {
                return true;
            }
        }
        return false;
    }
}
