package com.cashflow.app.model;

import java.time.LocalDate;

public class Sparziel {

    private int sparzielId;
    private String titel;
    private double zielBetrag;           // Gesamtbetrag, den man sparen möchte
    private LocalDate faelligBis;        // Zieltermin für das Sparziel
    private String beschreibung;

    private double aktuellerBetrag = 0.0; // Bisher gesparter Betrag

    // 🔹 Zuordnung: ENTWEDER einzelner Nutzer ODER Haushaltsgruppe
    private Nutzer nutzer;
    private Haushaltsgruppe haushaltsgruppe;

    // 🔹 Konstruktoren

    public Sparziel() {}

    // Sparziel für einzelne Person
    public Sparziel(int sparzielId, String titel, double zielBetrag, LocalDate faelligBis, String beschreibung, Nutzer nutzer) {
        this.sparzielId = sparzielId;
        this.titel = titel;
        this.zielBetrag = zielBetrag;
        this.faelligBis = faelligBis;
        this.beschreibung = beschreibung;
        this.nutzer = nutzer;
    }

    // Sparziel für eine Gruppe
    public Sparziel(int sparzielId, String titel, double zielBetrag, LocalDate faelligBis, String beschreibung, Haushaltsgruppe haushaltsgruppe) {
        this.sparzielId = sparzielId;
        this.titel = titel;
        this.zielBetrag = zielBetrag;
        this.faelligBis = faelligBis;
        this.beschreibung = beschreibung;
        this.haushaltsgruppe = haushaltsgruppe;
    }

    // 🔹 Hilfsmethoden

    /**
     * Fügt einen Betrag zum aktuellen Stand hinzu.
     */
    
    public void betragHinzufuegen(double betrag) {
        this.aktuellerBetrag += betrag;
    }

    
    public void beitragHinzufuegen(double betrag) {
        this.aktuellerBetrag += betrag;
    }

    /**
     * Gibt den noch fehlenden Betrag zum Erreichen des Ziels zurück.
     */
    public double getRestbetrag() {
        return Math.max(0, zielBetrag - aktuellerBetrag);
    }

    /**
     * Prüft, ob das Sparziel erreicht wurde.
     */
    public boolean istErreicht() {
        return aktuellerBetrag >= zielBetrag;
    }

    /**
     * Prüft, ob das Sparziel überfällig ist (Termin verstrichen und Ziel nicht erreicht).
     */
    public boolean istUeberfaellig(LocalDate heute) {
        return !istErreicht() && heute.isAfter(faelligBis);
    }

    /**
     * Gibt den prozentualen Fortschritt des Sparziels zurück.
     */
    public double getProzentualerFortschritt() {
        if (zielBetrag <= 0) return 0.0;
        return Math.min(100.0, (aktuellerBetrag / zielBetrag) * 100.0);
    }

    /**
     * Gibt zurück, ob es sich um ein persönliches Sparziel handelt.
     */
    public boolean istPersoenlich() {
        return nutzer != null;
    }

    /**
     * Gibt zurück, ob es sich um ein gruppenbasiertes Sparziel handelt.
     */
    public boolean istGruppenbasiert() {
        return haushaltsgruppe != null;
    }

    // 🔹 Getter und Setter

    public int getSparzielId() {
        return sparzielId;
    }
    
    public void setSparzielId(int sparzielId) {
    	this.sparzielId = sparzielId;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public double getZielBetrag() {
        return zielBetrag;
    }

    public void setZielBetrag(double zielBetrag) {
        this.zielBetrag = zielBetrag;
    }

    public LocalDate getFaelligBis() {
        return faelligBis;
    }

    public void setFaelligBis(LocalDate faelligBis) {
        this.faelligBis = faelligBis;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public double getAktuellerBetrag() {
        return aktuellerBetrag;
    }

    public void setAktuellerBetrag(double aktuellerBetrag) {
        this.aktuellerBetrag = aktuellerBetrag;
    }

    public Nutzer getNutzer() {
        return nutzer;
    }

    public void setNutzer(Nutzer nutzer) {
        this.nutzer = nutzer;
        this.haushaltsgruppe = null; // Stellt sicher, dass nur eine Zuordnung existiert
    }

    public Haushaltsgruppe getHaushaltsgruppe() {
        return haushaltsgruppe;
    }

    public void setHaushaltsgruppe(Haushaltsgruppe haushaltsgruppe) {
        this.haushaltsgruppe = haushaltsgruppe;
        this.nutzer = null; // Stellt sicher, dass nur eine Zuordnung existiert
    }
}
