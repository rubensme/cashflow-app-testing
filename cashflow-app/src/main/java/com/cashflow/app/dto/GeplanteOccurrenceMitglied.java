package com.cashflow.app.dto;

import java.time.LocalDate;

public class GeplanteOccurrenceMitglied {
    private LocalDate datum;
    private String titel;
    private double betrag;
    private String mitglied;

    public GeplanteOccurrenceMitglied(LocalDate datum, String titel, double betrag, String mitglied) {
        this.datum = datum; this.titel = titel; this.betrag = betrag; this.mitglied = mitglied;
    }
    public LocalDate getDatum() { return datum; }
    public String getTitel() { return titel; }
    public double getBetrag() { return betrag; }
    public String getMitglied() { return mitglied; }
}
