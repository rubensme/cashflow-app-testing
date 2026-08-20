package com.cashflow.app.model;

import java.time.LocalDate;

public class GeplanteTransaktion {
    private int geplanteId;
    private int nutzerId;
    private String titel;
    private LocalDate startDatum;
    private double betrag;   // positivo=entrada, negativo=saída
    private String periode;  // EINMALIG | MONATLICH | VIERTELJAEHRLICH | JAEHRLICH
    private String status;   // OFFEN | BEZAHLT

    public int getGeplanteId() { return geplanteId; }
    public void setGeplanteId(int geplanteId) { this.geplanteId = geplanteId; }

    public int getNutzerId() { return nutzerId; }
    public void setNutzerId(int nutzerId) { this.nutzerId = nutzerId; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public LocalDate getStartDatum() { return startDatum; }
    public void setStartDatum(LocalDate startDatum) { this.startDatum = startDatum; }

    public double getBetrag() { return betrag; }
    public void setBetrag(double betrag) { this.betrag = betrag; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
