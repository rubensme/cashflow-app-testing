package com.cashflow.app.model;

public class HaushaltsgruppeMitglied {
    private int gruppeId;
    private int nutzerId;
    private String vorname;
    private String nachname;
    private String email;
    private boolean aktiv;

    public int getGruppeId() { return gruppeId; }
    public void setGruppeId(int gruppeId) { this.gruppeId = gruppeId; }

    public int getNutzerId() { return nutzerId; }
    public void setNutzerId(int nutzerId) { this.nutzerId = nutzerId; }

    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }

    public String getNachname() { return nachname; }
    public void setNachname(String nachname) { this.nachname = nachname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }
}
