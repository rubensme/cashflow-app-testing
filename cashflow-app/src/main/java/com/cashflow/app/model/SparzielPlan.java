package com.cashflow.app.model;

import java.time.LocalDate;

/**
 * Plano operacional de um Sparziel: periodicidade, valores por período
 * e controle da próxima/última competência.
 */
public class SparzielPlan {

    private int planId;
    private int sparzielId;

    /** TAEGLICH | WOECHENTLICH | MONATLICH | VIERTELJAEHRLICH | HALBJAEHRLICH | JAEHRLICH */
    private String periode;

    /** Quantos membros contribuem neste Sparziel (1 para pessoal). */
    private int mitgliederAnzahl;

    /** Valor que cada membro deposita por período. */
    private double beitragProMitglied;

    /** Valor total do grupo por período (mitgliederAnzahl * beitragProMitglied). */
    private double beitragProGruppe;

    /** Próxima data de depósito (competência). */
    private LocalDate naechsteRate;

    /** Última competência realizada (pode ser null). */
    private LocalDate letzteRate;

    // ===== Construtores =====
    public SparzielPlan() {}

    public SparzielPlan(int planId,
                        int sparzielId,
                        String periode,
                        int mitgliederAnzahl,
                        double beitragProMitglied,
                        double beitragProGruppe,
                        LocalDate naechsteRate,
                        LocalDate letzteRate) {
        this.planId = planId;
        this.sparzielId = sparzielId;
        this.periode = periode;
        this.mitgliederAnzahl = mitgliederAnzahl;
        this.beitragProMitglied = beitragProMitglied;
        this.beitragProGruppe = beitragProGruppe;
        this.naechsteRate = naechsteRate;
        this.letzteRate = letzteRate;
    }

    // ===== Getters/Setters =====
    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getSparzielId() {
        return sparzielId;
    }

    public void setSparzielId(int sparzielId) {
        this.sparzielId = sparzielId;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public int getMitgliederAnzahl() {
        return mitgliederAnzahl;
    }

    public void setMitgliederAnzahl(int mitgliederAnzahl) {
        this.mitgliederAnzahl = mitgliederAnzahl;
    }

    public double getBeitragProMitglied() {
        return beitragProMitglied;
    }

    public void setBeitragProMitglied(double beitragProMitglied) {
        this.beitragProMitglied = beitragProMitglied;
    }

    public double getBeitragProGruppe() {
        return beitragProGruppe;
    }

    public void setBeitragProGruppe(double beitragProGruppe) {
        this.beitragProGruppe = beitragProGruppe;
    }

    public LocalDate getNaechsteRate() {
        return naechsteRate;
    }

    public void setNaechsteRate(LocalDate naechsteRate) {
        this.naechsteRate = naechsteRate;
    }

    public LocalDate getLetzteRate() {
        return letzteRate;
    }

    public void setLetzteRate(LocalDate letzteRate) {
        this.letzteRate = letzteRate;
    }

    @Override
    public String toString() {
        return "SparzielPlan{" +
                "planId=" + planId +
                ", sparzielId=" + sparzielId +
                ", periode='" + periode + '\'' +
                ", mitgliederAnzahl=" + mitgliederAnzahl +
                ", beitragProMitglied=" + beitragProMitglied +
                ", beitragProGruppe=" + beitragProGruppe +
                ", naechsteRate=" + naechsteRate +
                ", letzteRate=" + letzteRate +
                '}';
    }
}
