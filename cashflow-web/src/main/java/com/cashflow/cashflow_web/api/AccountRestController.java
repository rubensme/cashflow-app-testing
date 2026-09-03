package com.cashflow.cashflow_web.api;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Nutzer;
import com.cashflow.cashflow_web.api.dto.AccountResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {

    private final BankkontoDAO bankkontoDAO;

    public AccountRestController(BankkontoDAO bankkontoDAO) {
        this.bankkontoDAO = bankkontoDAO;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            HttpSession session
    ) {
        Object sessionUser = session.getAttribute("eingeloggterNutzer");

        if (!(sessionUser instanceof Nutzer nutzer)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        try {
            List<AccountResponse> response = bankkontoDAO
                    .findByNutzerId(nutzer.getNutzerId())
                    .stream()
                    .map(this::toResponse)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (SQLException exception) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private AccountResponse toResponse(Bankkonto konto) {
        return new AccountResponse(
                konto.getBankKontoId(),
                konto.getNameDerBank(),
                konto.getAktuellerSaldo(),
                konto.isChecked()
        );
    }
}