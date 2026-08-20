package com.cashflow.app.dto;

import java.time.LocalDate;

public class GeplanteOccurrence {
    private final LocalDate datum;
    private final String titel;
    private final double betrag; // positivo=entrada, negativo=saída

    public GeplanteOccurrence(LocalDate datum, String titel, double betrag) {
        this.datum = datum;
        this.titel = titel;
        this.betrag = betrag;
    }

    public LocalDate getDatum() { return datum; }
    public String getTitel() { return titel; }
    public double getBetrag() { return betrag; }
}
