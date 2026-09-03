package com.cashflow.cashflow_web.api.dto;

public record AccountResponse(
        int accountId,
        String name,
        double balance,
        boolean selected
) {
}