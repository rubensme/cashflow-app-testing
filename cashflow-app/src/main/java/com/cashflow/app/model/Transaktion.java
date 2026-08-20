package com.cashflow.app.model;

import java.time.LocalDate;

public class Transaktion {

    private int transaktionId;
    private LocalDate datum;
    private double betrag;
    private String beschreibung;         // Ex: "Miete April", "Rewe", "Amazon"
    private String empfaenger;           // Nome do destinatário, se disponível
    private String kategorie;            // Opcional: alimentação, moradia, transporte etc.

    private Bankkonto konto;             // Conta à qual pertence a transação
    private Integer zugeordneteAusgabeId; // ID da FesteAusgabe associada, se houver

    // 🔹 Construtores

    public Transaktion() {}

    public Transaktion(int transaktionId, LocalDate datum, double betrag, String beschreibung, String empfaenger, Bankkonto konto) {
        this.transaktionId = transaktionId;
        this.datum = datum;
        this.betrag = betrag;
        this.beschreibung = beschreibung;
        this.empfaenger = empfaenger;
        this.konto = konto;
    }
    
    public Transaktion(int transaktionId, String beschreibung, double betrag, LocalDate datum, String kategorie, Bankkonto konto) {
        this.transaktionId = transaktionId;
        this.beschreibung = beschreibung;
        this.betrag = betrag;
        this.datum = datum;
        this.kategorie = kategorie;
        this.konto = konto;
    }

    // 🔹 Getters und Setters

    public int getTransaktionId() {
        return transaktionId;
    }

    public void setTransaktionId(int transaktionId) {
        this.transaktionId = transaktionId;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public double getBetrag() {
        return betrag;
    }

    public void setBetrag(double betrag) {
        this.betrag = betrag;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getEmpfaenger() {
        return empfaenger;
    }

    public void setEmpfaenger(String empfaenger) {
        this.empfaenger = empfaenger;
    }

    public String getKategorie() {
        return kategorie;
    }

    public void setKategorie(String kategorie) {
        this.kategorie = kategorie;
    }

    public Bankkonto getKonto() {
        return konto;
    }

    public void setKonto(Bankkonto konto) {
        this.konto = konto;
    }

    public Integer getZugeordneteAusgabeId() {
        return zugeordneteAusgabeId;
    }

    public void setZugeordneteAusgabeId(Integer zugeordneteAusgabeId) {
        this.zugeordneteAusgabeId = zugeordneteAusgabeId;
    }
    
 // em com.cashflow.app.model.Transaktion
    private String quelle;

    public String getQuelle() {
        return quelle;
    }

    public void setQuelle(String quelle) {
        this.quelle = quelle;
    }


    // 🔹 Utilitário

    public boolean istMitFesterAusgabeVerknuepft() {
        return zugeordneteAusgabeId != null;
    }
}
