package com.cashflow.app.model;

import java.time.LocalDate;

public class FesteAusgabe {

    private int festeAusgabeId;
    private String titel;
    private double betrag;
    private LocalDate naechsteFaelligkeit;
    private Rhythmus rhythmus;
    private boolean bestaetigt;
    private String kategorie;

    private int nutzerId; // 🔹 Adicionado campo de identificação do usuário

    public enum Rhythmus {
        EINMALIG,
        MONATLICH,
        JAEHRLICH
    }

    public FesteAusgabe(int festeAusgabeId, String titel, double betrag, LocalDate naechsteFaelligkeit, Rhythmus rhythmus) {
        this.festeAusgabeId = festeAusgabeId;
        this.titel = titel;
        this.betrag = betrag;
        this.naechsteFaelligkeit = naechsteFaelligkeit;
        this.rhythmus = rhythmus;
        this.bestaetigt = false;
    }

    // 🔹 Getters e Setters

    public int getFesteAusgabeId() {
        return festeAusgabeId;
    }

    public void setFesteAusgabeID(int festeAusgabeId) {
        this.festeAusgabeId = festeAusgabeId;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public double getBetrag() {
        return betrag;
    }

    public void setBetrag(double betrag) {
        this.betrag = betrag;
    }

    public LocalDate getNaechsteFaelligkeit() {
        return naechsteFaelligkeit;
    }

    public void setNaechsteFaelligkeit(LocalDate datum) {
        this.naechsteFaelligkeit = datum;
    }

    public Rhythmus getRhythmus() {
        return rhythmus;
    }

    public void setRhythmus(Rhythmus rhythmus) {
        this.rhythmus = rhythmus;
    }

    public boolean isBestaetigt() {
        return bestaetigt;
    }

    public void setBestaetigt(boolean bestaetigt) {
        this.bestaetigt = bestaetigt;
    }

    public String getKategorie() {
        return kategorie;
    }

    public void setKategorie(String kategorie) {
        this.kategorie = kategorie;
    }

    // 🔹 Getter e Setter para nutzerId

    public int getNutzerId() {
        return nutzerId;
    }

    public void setNutzerId(int nutzerId) {
        this.nutzerId = nutzerId;
    }

    // 🔹 Métodos auxiliares

    public boolean faelligImMonat(LocalDate bisDatum) {
        return !naechsteFaelligkeit.isAfter(bisDatum);
    }

    public void aktualisiereNaechsteFaelligkeit() {
        switch (rhythmus) {
            case MONATLICH:
                naechsteFaelligkeit = naechsteFaelligkeit.plusMonths(1);
                break;
            case JAEHRLICH:
                naechsteFaelligkeit = naechsteFaelligkeit.plusYears(1);
                break;
            case EINMALIG:
                // Nada a fazer
                break;
        }
    }
}
