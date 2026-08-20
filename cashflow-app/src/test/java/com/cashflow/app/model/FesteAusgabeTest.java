package com.cashflow.app.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FesteAusgabeTest {

    @Test
    void sollteNaechsteFaelligkeitBeiMonatlichemRhythmusUmEinenMonatErhoehen() {

        // Arrange
        FesteAusgabe ausgabe = new FesteAusgabe(
                1,
                "Miete",
                800.0,
                LocalDate.of(2026, 8, 15),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        // Act
        ausgabe.aktualisiereNaechsteFaelligkeit();

        // Assert
        assertEquals(
                LocalDate.of(2026, 9, 15),
                ausgabe.getNaechsteFaelligkeit()
        );
    }

    @Test
    void sollteNaechsteFaelligkeitBeiJaehrlichemRhythmusUmEinJahrErhoehen() {

        // Arrange
        FesteAusgabe ausgabe = new FesteAusgabe(
                2,
                "Versicherung",
                1200.0,
                LocalDate.of(2026, 8, 15),
                FesteAusgabe.Rhythmus.JAEHRLICH
        );

        // Act
        ausgabe.aktualisiereNaechsteFaelligkeit();

        // Assert
        assertEquals(
                LocalDate.of(2027, 8, 15),
                ausgabe.getNaechsteFaelligkeit()
        );
    }

    @Test
    void sollteMonatsendeBeiMonatlichemRhythmusKorrektBehandeln() {

        // Arrange
        FesteAusgabe ausgabe = new FesteAusgabe(
                3,
                "Miete",
                800.0,
                LocalDate.of(2026, 1, 31),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        // Act
        ausgabe.aktualisiereNaechsteFaelligkeit();

        // Assert
        assertEquals(
                LocalDate.of(2026, 2, 28),
                ausgabe.getNaechsteFaelligkeit()
        );
    }

    @Test
    void sollteAmStichtagFaelligSein() {

        // Arrange
        FesteAusgabe ausgabe = new FesteAusgabe(
                4,
                "Strom",
                100.0,
                LocalDate.of(2026, 8, 31),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        LocalDate bisDatum = LocalDate.of(2026, 8, 31);

        // Act
        boolean istFaellig = ausgabe.faelligImMonat(bisDatum);

        // Assert
        assertTrue(istFaellig);
    }

    @Test
    void sollteNachDemStichtagNichtFaelligSein() {

        // Arrange
        FesteAusgabe ausgabe = new FesteAusgabe(
                5,
                "Strom",
                100.0,
                LocalDate.of(2026, 9, 1),
                FesteAusgabe.Rhythmus.MONATLICH
        );

        LocalDate bisDatum = LocalDate.of(2026, 8, 31);

        // Act
        boolean istFaellig = ausgabe.faelligImMonat(bisDatum);

        // Assert
        assertFalse(istFaellig);
    }
}