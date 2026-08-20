package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.dao.CashTransaktionDAO;
import com.cashflow.app.dao.TransaktionDAO;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Nutzer;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class KontoAktionenController {

    private final BankkontoDAO bankkontoDAO = new BankkontoDAO();
    private final TransaktionDAO transaktionDAO = new TransaktionDAO();
    private final CashTransaktionDAO cashTransaktionDAO = new CashTransaktionDAO();

    // ===== Saldo anpassen (Bankkonto) =====
    @PostMapping("/konto/{id}/saldo/abgleichen")
    public String kontoSaldoAbgleichen(@PathVariable("id") int kontoId,
                                       @RequestParam(value = "neuerSaldo", required = false) Double neuerSaldo,
                                       HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";
        int nutzerId = n.getNutzerId();

        // Campo vazio -> não faz nada
        if (neuerSaldo == null) return "redirect:/home";

        Bankkonto k = bankkontoDAO.findById(kontoId);
        if (k == null || k.getNutzer() == null || k.getNutzer().getNutzerId() != nutzerId) {
            return "redirect:/home";
        }

        double alt = k.getAktuellerSaldo();
        double diff = round2(neuerSaldo - alt);

        if (Math.abs(diff) > 0.0001) {
            transaktionDAO.insertManuellerAbgleichFuerKonto(
                    nutzerId, kontoId, LocalDate.now(), diff, "Manueller Saldoabgleich");
            bankkontoDAO.updateSaldoSafe(kontoId, neuerSaldo);
        }
        return "redirect:/home";
    }

    // ===== Transaktion einfügen (Bankkonto) =====
    @PostMapping("/konto/{id}/transaktion/anlegen")
    public String kontoTransaktionAnlegen(@PathVariable("id") int kontoId,
                                          @RequestParam("datum") String datum,
                                          @RequestParam(value = "titel", required = false) String titel,
                                          @RequestParam(value = "betrag", required = false) Double betrag,
                                          HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";
        int nutzerId = n.getNutzerId();

        // Campo vazio -> não faz nada
        if (betrag == null) return "redirect:/home";

        Bankkonto k = bankkontoDAO.findById(kontoId);
        if (k == null || k.getNutzer() == null || k.getNutzer().getNutzerId() != nutzerId) {
            return "redirect:/home";
        }

        LocalDate d = parseDate(datum);
        String desc = (titel == null) ? "" : titel.trim();

        transaktionDAO.insertManuelleTransaktionFuerKonto(nutzerId, kontoId, d, betrag, desc);

        double neuerSaldo = round2(k.getAktuellerSaldo() + betrag);
        bankkontoDAO.updateSaldoSafe(kontoId, neuerSaldo);

        return "redirect:/home";
    }

    // ===== Saldo anpassen (Cash) =====
    @PostMapping("/cash/saldo/abgleichen")
    public String cashSaldoAbgleichen(@RequestParam(value = "neuerSaldo", required = false) Double neuerSaldo,
                                      HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";
        int nutzerId = n.getNutzerId();

        if (neuerSaldo == null) return "redirect:/home";

        double alt = cashTransaktionDAO.sumCashSaldoByNutzerId(nutzerId);
        double diff = round2(neuerSaldo - alt);
        if (Math.abs(diff) > 0.0001) {
            transaktionDAO.insertCashTransaktion(
                    nutzerId, LocalDate.now(), diff, "Manueller Saldoabgleich");
        }
        return "redirect:/home";
    }

    // ===== Transaktion einfügen (Cash) =====
    @PostMapping("/cash/transaktion/anlegen")
    public String cashTransaktionAnlegen(@RequestParam("datum") String datum,
                                         @RequestParam(value = "titel", required = false) String titel,
                                         @RequestParam(value = "betrag", required = false) Double betrag,
                                         HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        if (betrag == null) return "redirect:/home";

        LocalDate d = parseDate(datum);
        String desc = (titel == null) ? "" : titel.trim();

        transaktionDAO.insertCashTransaktion(n.getNutzerId(), d, betrag, desc);
        return "redirect:/home";
    }

    // ==== helpers ====
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
    private static LocalDate parseDate(String s) {
        try {
            String x = (s == null) ? "" : s.trim();
            if (x.length() >= 10) x = x.substring(0, 10);
            return x.isEmpty() ? LocalDate.now() : LocalDate.parse(x);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
