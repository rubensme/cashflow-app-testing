package com.cashflow.app.model;

import java.time.LocalDate;

/**
 * Marca que um usuário já confirmou a contribuição de uma competência (rate)
 * de um determinado Sparziel — evita contagem dupla.
 */
public class SparzielUserMark {

    private int markId;
    private int sparzielId;
    private int userId;

    /** Data (competência) à qual a marca se refere. */
    private LocalDate rateDatum;

    // ===== Construtores =====
    public SparzielUserMark() {}

    public SparzielUserMark(int markId, int sparzielId, int userId, LocalDate rateDatum) {
        this.markId = markId;
        this.sparzielId = sparzielId;
        this.userId = userId;
        this.rateDatum = rateDatum;
    }

    // ===== Getters/Setters =====
    public int getMarkId() {
        return markId;
    }

    public void setMarkId(int markId) {
        this.markId = markId;
    }

    public int getSparzielId() {
        return sparzielId;
    }

    public void setSparzielId(int sparzielId) {
        this.sparzielId = sparzielId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDate getRateDatum() {
        return rateDatum;
    }

    public void setRateDatum(LocalDate rateDatum) {
        this.rateDatum = rateDatum;
    }

    @Override
    public String toString() {
        return "SparzielUserMark{" +
                "markId=" + markId +
                ", sparzielId=" + sparzielId +
                ", userId=" + userId +
                ", rateDatum=" + rateDatum +
                '}';
    }
}
