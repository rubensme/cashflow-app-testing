package com.cashflow.app.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.cashflow.app.service.PrognoseService;

public class Bankkonto {

    private int bankKontoId;
    private String nameDerBank;
    private String iban;
    private String kontonummer;
    private String bic;
    private double aktuellerSaldo;
    private String waehrung;

    private Nutzer nutzer;
    private List<Transaktion> transaktionen = new ArrayList<>();

    // 🔹 Registra IDs de FesteAusgabe confirmadas
    private List<Integer> bestaetigteAusgabenIds = new ArrayList<>();

    // 🔹 Construtores
    public Bankkonto() {}

    public Bankkonto(int bankKontoId, String nameDerBank, String iban, String kontonummer, String bic,
                     double aktuellerSaldo, String waehrung, Nutzer nutzer) {
        this.bankKontoId = bankKontoId;
        this.nameDerBank = nameDerBank;
        this.iban = iban;
        this.kontonummer = kontonummer;
        this.bic = bic;
        this.aktuellerSaldo = aktuellerSaldo;
        this.waehrung = waehrung;
        this.nutzer = nutzer;
    }

    // 🔹 MÉTODOS AUXILIARES

    public void aktualisiereSaldo(double betrag) {
        this.aktuellerSaldo += betrag;
    }

    public void transaktionHinzufuegen(Transaktion t) {
        transaktionen.add(t);
        aktualisiereSaldo(t.getBetrag());
    }

    public void transaktionEntfernen(Transaktion t) {
        if (transaktionen.remove(t)) {
            aktualisiereSaldo(-t.getBetrag());
        }
    }

    public void berechneAktuellenSaldo() {
        double summe = 0;
        for (Transaktion t : transaktionen) {
            summe += t.getBetrag();
        }
        this.aktuellerSaldo = summe;
    }

    // 🔹 Confirma pagamento de uma FesteAusgabe
    public void bestaetigeFesteAusgabe(int ausgabeId) {
        if (!bestaetigteAusgabenIds.contains(ausgabeId)) {
            bestaetigteAusgabenIds.add(ausgabeId);
        }
    }

    public boolean istAusgabeBestaetigt(int ausgabeId) {
        return bestaetigteAusgabenIds.contains(ausgabeId);
    }

    // 🔹 Prognose-Service-Funktionen

    public double getPrognostizierterSaldoBis(LocalDate bisDatum) {
        PrognoseService service = new PrognoseService();
        return service.berechnePrognostizierterSaldo(this, bisDatum);
    }

    public List<FesteAusgabe> getOffeneAusgabenBis(LocalDate bisDatum) {
        PrognoseService service = new PrognoseService();
        return service.getOffeneAusgabenImZeitraum(this, bisDatum);
    }

    // 🔹 Getters und Setters

    public int getBankKontoId() {
        return bankKontoId;
    }

    public void setBankKontoId(int bankKontoId) {
        this.bankKontoId = bankKontoId;
    }

    public String getNameDerBank() {
        return nameDerBank;
    }

    public void setNameDerBank(String nameDerBank) {
        this.nameDerBank = nameDerBank;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getKontonummer() {
        return kontonummer;
    }

    public void setKontonummer(String kontonummer) {
        this.kontonummer = kontonummer;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public double getAktuellerSaldo() {
        return aktuellerSaldo;
    }

    public void setAktuellerSaldo(double aktuellerSaldo) {
        this.aktuellerSaldo = aktuellerSaldo;
    }

    public String getWaehrung() {
        return waehrung;
    }

    public void setWaehrung(String waehrung) {
        this.waehrung = waehrung;
    }

    public Nutzer getNutzer() {
        return nutzer;
    }

    public void setNutzer(Nutzer nutzer) {
        this.nutzer = nutzer;
    }

    public List<Transaktion> getTransaktionen() {
        return transaktionen;
    }

    public void setTransaktionen(List<Transaktion> transaktionen) {
        this.transaktionen = transaktionen;
    }

    public List<Integer> getBestaetigteAusgabenIds() {
        return bestaetigteAusgabenIds;
    }

    public void setBestaetigteAusgabenIds(List<Integer> bestaetigteAusgabenIds) {
        this.bestaetigteAusgabenIds = bestaetigteAusgabenIds;
    }
    
    private boolean isChecked;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

}
