package com.cashflow.app.model;

import com.cashflow.app.service.PrognoseService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Nutzer {

    private int nutzerId;
    private String vorname;
    private String nachname;
    private String email;
    private String passwort;

    // Relacionamentos
    private List<Bankkonto> konten = new ArrayList<>();
    private List<Transaktion> transaktionen = new ArrayList<>();
    private List<Sparziel> sparziele = new ArrayList<>();
    private List<FesteAusgabe> festeAusgaben = new ArrayList<>();

    // Construtores
    public Nutzer() {}

    public Nutzer(int nutzerId, String vorname, String nachname, String email, String passwort) {
        this.nutzerId = nutzerId;
        this.vorname = vorname;
        this.nachname = nachname;
        this.email = email;
        this.passwort = passwort;
    }

    // Getters e Setters
    public int getNutzerId() { return nutzerId; }
    public void setNutzerId(int nutzerId) { 
    	this.nutzerId = nutzerId; 
    	}

    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }

    public String getNachname() { return nachname; }
    public void setNachname(String nachname) { this.nachname = nachname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswort() { return passwort; }
    public void setPasswort(String passwort) { this.passwort = passwort; }

    public List<Bankkonto> getKonten() { return konten; }
    public List<Transaktion> getTransaktionen() { return transaktionen; }
    public List<Sparziel> getSparziele() { return sparziele; }
    public List<FesteAusgabe> getFesteAusgaben() { return festeAusgaben; }

    // Métodos auxiliares
    public void kontoHinzufuegen(Bankkonto konto) {
        konten.add(konto);
    }

    public void transaktionHinzufuegen(Transaktion transaktion) {
        transaktionen.add(transaktion);
    }

    public void sparzielHinzufuegen(Sparziel sparziel) {
        sparziele.add(sparziel);
    }

    public void festeAusgabeHinzufuegen(FesteAusgabe ausgabe) {
        festeAusgaben.add(ausgabe);
    }

    // 🔹 Integração com PrognoseService
    public double berechneGesamtPrognoseBis(LocalDate bisDatum) {
        PrognoseService prognoseService = new PrognoseService();
        double summe = 0;
        for (Bankkonto konto : konten) {
            summe += prognoseService.berechnePrognostizierterSaldo(konto, bisDatum);
        }
        return summe;
    }

    public List<FesteAusgabe> getAlleOffenenAusgabenBis(LocalDate bisDatum) {
        PrognoseService prognoseService = new PrognoseService();
        List<FesteAusgabe> alleOffene = new ArrayList<>();
        for (Bankkonto konto : konten) {
            alleOffene.addAll(prognoseService.getOffeneAusgabenImZeitraum(konto, bisDatum));
        }
        return alleOffene;
    }
}
