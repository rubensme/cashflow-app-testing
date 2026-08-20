package com.cashflow.app.model;

public class CashTransaktion {

    private int cashTransaktionId;
    private String datum;          // formato: "yyyy-MM-dd" ou similar
    private String beschreibung;
    private double betrag;
    private int nutzerId;

    public CashTransaktion() {
    }

    public CashTransaktion(int cashTransaktionId, String datum, String beschreibung, double betrag, int nutzerId) {
        this.cashTransaktionId = cashTransaktionId;
        this.datum = datum;
        this.beschreibung = beschreibung;
        this.betrag = betrag;
        this.nutzerId = nutzerId;
    }

    public int getCashTransaktionId() {
        return cashTransaktionId;
    }

    public void setCashTransaktionId(int cashTransaktionId) {
        this.cashTransaktionId = cashTransaktionId;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public double getBetrag() {
        return betrag;
    }

    public void setBetrag(double betrag) {
        this.betrag = betrag;
    }

    public int getNutzerId() {
        return nutzerId;
    }

    public void setNutzerId(int nutzerId) {
        this.nutzerId = nutzerId;
    }

    @Override
    public String toString() {
        return "CashTransaktion{" +
                "cashTransaktionId=" + cashTransaktionId +
                ", datum='" + datum + '\'' +
                ", beschreibung='" + beschreibung + '\'' +
                ", betrag=" + betrag +
                ", nutzerId=" + nutzerId +
                '}';
    }
}
