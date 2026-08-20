package com.cashflow.app.model;

public class Haushaltsgruppe {

    // Mantemos o mesmo campo antigo e criamos aliases para compatibilidade
    private int haushaltsgruppeId;     // id físico
    private String name;               // título/nome do grupo

    // NOVOS CAMPOS
    private String beschr;             // descrição
    private int ownerNutzerId;         // dono do grupo
    private String invitedEmails;      // e-mails convidados (\n separados)
    private boolean invitesPending;    // há convites pendentes a enviar?

    public Haushaltsgruppe() {}

    public Haushaltsgruppe(int id, String name) {
        this.haushaltsgruppeId = id;
        this.name = name;
    }

    // ===================== IDs (retrocompat) =====================

    // Antigo
    public int getHaushaltsgruppeId() {
        return haushaltsgruppeId;
    }
    public void setHaushaltsgruppeId(int haushaltsgruppeId) {
        this.haushaltsgruppeId = haushaltsgruppeId;
    }

    // Alias novo usado no DAO/Controller
    public int getGruppeId() {
        return haushaltsgruppeId;
    }
    public void setGruppeId(int gruppeId) {
        this.haushaltsgruppeId = gruppeId;
    }

    // ===================== Nome =====================

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // ===================== NOVOS CAMPOS =====================

    public String getBeschr() {
        return beschr;
    }
    public void setBeschr(String beschr) {
        this.beschr = beschr;
    }
    
 

    // aliases para compatibilidade
    public String getBeschreibung() { return beschr; }
    public void setBeschreibung(String beschr) { this.beschr = beschr; }


    public int getOwnerNutzerId() {
        return ownerNutzerId;
    }
    public void setOwnerNutzerId(int ownerNutzerId) {
        this.ownerNutzerId = ownerNutzerId;
    }

    public String getInvitedEmails() {
        return invitedEmails;
    }
    public void setInvitedEmails(String invitedEmails) {
        this.invitedEmails = invitedEmails;
    }

    public boolean isInvitesPending() {
        return invitesPending;
    }
    public void setInvitesPending(boolean invitesPending) {
        this.invitesPending = invitesPending;
    }
}
