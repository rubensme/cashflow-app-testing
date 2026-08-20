package com.cashflow.app.service;

import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.FesteAusgabe;
import com.cashflow.app.model.Nutzer;
import com.cashflow.app.model.Transaktion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrognoseServiceTest {

    private Nutzer nutzer;
    private Bankkonto konto;
    private PrognoseService service;

    @BeforeEach
    void setUp() {

        nutzer = new Nutzer(
                1,
                "Max",
                "Mustermann",
                "max@example.com",
                "geheim"
        );

        konto = new Bankkonto(
                1,
                "Sparkasse",
                "DE0012345678",
                "12345678",
                "BYLADEM1",
                1000.0,
                "EUR",
                nutzer
        );

        service = new PrognoseService();
    }

    @Test
    void sollteBereitsBezahlteFesteAusgabeNichtNochEinmalAbziehen() {

     //Arrange
        FesteAusgabe miete = new FesteAusgabe(
                1,
                "Miete",
                100.0,
                LocalDate.of(2026, 8, 18),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        nutzer.festeAusgabeHinzufuegen(miete);

        Transaktion mieteBezahlt = new Transaktion(
                1,
                LocalDate.of(2026, 8, 18),
                -100.0,
                "Miete",
                "Wohnung",
                konto
        );

        konto.transaktionHinzufuegen(mieteBezahlt);


        // Act
        double prognose = service.berechnePrognostizierterSaldo(
                konto,
                LocalDate.of(2026, 8, 31)
        );

        // Assert
        assertEquals(900.0, prognose, 0.001);
    }
    @Test
    void sollteTransaktionEinenMonatVorFaelligkeitNichtAlsBezahlungErkennen() {

        // Arrange

        FesteAusgabe miete = new FesteAusgabe(
                2,
                "Miete",
                100.0,
                LocalDate.of(2026, 9, 18),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        nutzer.festeAusgabeHinzufuegen(miete);

        Transaktion alteTransaktion = new Transaktion(
                2,
                LocalDate.of(2026, 8, 18),
                -100.0,
                "Miete",
                "Wohnung",
                konto
        );

        konto.transaktionHinzufuegen(alteTransaktion);


        // Act
        double prognose = service.berechnePrognostizierterSaldo(
                konto,
                LocalDate.of(2026, 9, 30)
        );

        // Assert
        assertEquals(800.0, prognose, 0.001);
    }

    @Test
    void sollteTransaktionDreiTageVorFaelligkeitAlsBezahlungErkennen() {

        // Arrange


        FesteAusgabe miete = new FesteAusgabe(
                3,
                "Miete",
                100.0,
                LocalDate.of(2026, 9, 18),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        nutzer.festeAusgabeHinzufuegen(miete);

        Transaktion transaktion = new Transaktion(
                3,
                LocalDate.of(2026, 9, 15),
                -100.0,
                "Miete",
                "Wohnung",
                konto
        );

        konto.transaktionHinzufuegen(transaktion);


        // Act
        double prognose = service.berechnePrognostizierterSaldo(
                konto,
                LocalDate.of(2026, 9, 30)
        );

        // Assert
        assertEquals(900.0, prognose, 0.001);
    }

    @Test
    void sollteTransaktionVierTageVorFaelligkeitNichtAlsBezahlungErkennen() {

        // Arrange

        FesteAusgabe miete = new FesteAusgabe(
                4,
                "Miete",
                100.0,
                LocalDate.of(2026, 9, 18),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        nutzer.festeAusgabeHinzufuegen(miete);

        Transaktion transaktion = new Transaktion(
                4,
                LocalDate.of(2026, 9, 14),
                -100.0,
                "Miete",
                "Wohnung",
                konto
        );

        konto.transaktionHinzufuegen(transaktion);


        // Act
        double prognose = service.berechnePrognostizierterSaldo(
                konto,
                LocalDate.of(2026, 9, 30)
        );

        // Assert
        assertEquals(800.0, prognose, 0.001);
    }

}