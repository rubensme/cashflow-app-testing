package com.cashflow.app.model;

public class NutzerEinstellungen {
    private int nutzerId;
    private boolean includeCash;

    public NutzerEinstellungen(int nutzerId, boolean includeCash) {
        this.nutzerId = nutzerId;
        this.includeCash = includeCash;
    }

    public int getNutzerId() {
        return nutzerId;
    }

    public void setNutzerId(int nutzerId) {
        this.nutzerId = nutzerId;
    }

    public boolean isIncludeCash() {
        return includeCash;
    }

    public void setIncludeCash(boolean includeCash) {
        this.includeCash = includeCash;
    }
}
