package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.dao.HaushaltsgruppeDAO;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Haushaltsgruppe;
import com.cashflow.app.model.Nutzer;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.*;

@Controller
public class KontoController {

    private final BankkontoDAO bankkontoDAO = new BankkontoDAO();
    private final HaushaltsgruppeDAO haushaltsgruppeDAO = new HaushaltsgruppeDAO();

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // GET: página
    @GetMapping("/konto")
    public String konto(Model model, HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        // ===== Navbar: grupos do usuário =====
        try {
            List<Haushaltsgruppe> ownedGroups  = haushaltsgruppeDAO.findOwnedBy(n.getNutzerId());
            List<Haushaltsgruppe> memberGroups = haushaltsgruppeDAO.findMemberOfButNotOwner(n.getNutzerId());
            model.addAttribute("ownedGroups", ownedGroups == null ? Collections.emptyList() : ownedGroups);
            model.addAttribute("memberGroups", memberGroups == null ? Collections.emptyList() : memberGroups);
        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("ownedGroups", Collections.emptyList());
            model.addAttribute("memberGroups", Collections.emptyList());
        }

        // ===== Dados da página Konto =====
        List<Bankkonto> alle;
        try {
            alle = bankkontoDAO.findByNutzerId(n.getNutzerId());
        } catch (SQLException e) {
            e.printStackTrace();
            alle = Collections.emptyList();
        }

        // Particiona: Bankkonten (IBAN/BIC preenchidos) vs Custom (ambos vazios)
        List<Bankkonto> bankkonten = new ArrayList<>();
        List<Bankkonto> customKonten = new ArrayList<>();
        for (Bankkonto k : alle) {
            boolean custom = isBlank(k.getIban()) && isBlank(k.getBic());
            if (custom) customKonten.add(k); else bankkonten.add(k);
        }

        // Pré-separa nome/descrição das custom para o template
        Map<Integer,String> customNameMap = new HashMap<>();
        Map<Integer,String> customDescMap = new HashMap<>();
        for (Bankkonto k : customKonten) {
            String raw = k.getNameDerBank() == null ? "" : k.getNameDerBank();
            int open = raw.lastIndexOf('(');
            int close = raw.lastIndexOf(')');
            String base = raw;
            String desc = "";
            if (open > 0 && close > open) {
                base = raw.substring(0, open).trim();
                desc = raw.substring(open + 1, close).trim();
            }
            customNameMap.put(k.getBankKontoId(), base);
            customDescMap.put(k.getBankKontoId(), desc);
        }

        model.addAttribute("bankkonten", bankkonten);
        model.addAttribute("customKonten", customKonten);
        model.addAttribute("customNameMap", customNameMap);
        model.addAttribute("customDescMap", customDescMap);
        model.addAttribute("kryptoKonten", Collections.emptyList()); // placeholder

        boolean hasBank = !bankkonten.isEmpty();
        boolean hasCustom = !customKonten.isEmpty();
        boolean hasKrypto = false; // por enquanto
        boolean hasAny = hasBank || hasCustom || hasKrypto;

        model.addAttribute("hasBank", hasBank);
        model.addAttribute("hasCustom", hasCustom);
        model.addAttribute("hasKrypto", hasKrypto);
        model.addAttribute("hasAnyKonten", hasAny);

        return "konto";
    }

    // POST: adicionar BANK-KONTO
    @PostMapping("/konto/bank/hinzufuegen")
    public String addBankKonto(HttpSession session,
                               @RequestParam("bankname") String bankname,
                               @RequestParam("iban") String iban,
                               @RequestParam("bic") String bic,
                               RedirectAttributes ra) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        try {
            Bankkonto k = new Bankkonto();
            k.setNameDerBank(bankname == null ? "" : bankname.trim());
            k.setIban(iban == null ? "" : iban.trim());
            k.setBic(bic == null ? "" : bic.trim());
            k.setAktuellerSaldo(0.0);

            Nutzer nutzer = new Nutzer();
            nutzer.setNutzerId(n.getNutzerId());
            k.setNutzer(nutzer);

            bankkontoDAO.save(k);
            ra.addFlashAttribute("successAdd", "Konto wurde erfolgreich hinzugefügt.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "redirect:/konto";
    }

    // POST: adicionar BENUTZERDEFINIERTES KONTO
    @PostMapping("/konto/custom/hinzufuegen")
    public String addCustomKonto(HttpSession session,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "beschreibung", required = false) String beschreibung,
                                 RedirectAttributes ra) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        try {
            Bankkonto k = new Bankkonto();
            String bankname = (name == null ? "" : name.trim());
            if (beschreibung != null && !beschreibung.isBlank()) {
                bankname = bankname + " (" + beschreibung.trim() + ")";
            }
            k.setNameDerBank(bankname);
            k.setIban("");
            k.setBic("");
            k.setAktuellerSaldo(0.0);

            Nutzer nutzer = new Nutzer();
            nutzer.setNutzerId(n.getNutzerId());
            k.setNutzer(nutzer);

            bankkontoDAO.save(k);
            ra.addFlashAttribute("successAdd", "Konto wurde erfolgreich hinzugefügt.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "redirect:/konto";
    }

    // POST: adicionar KRYPTOKONTO (placeholder)
    @PostMapping("/konto/krypto/hinzufuegen")
    public String addKryptoKonto(HttpSession session,
                                 @RequestParam(value = "name", required = false) String name,
                                 @RequestParam(value = "asset", required = false) String asset,
                                 @RequestParam(value = "preisquelle", required = false) String preisquelle,
                                 RedirectAttributes ra) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        ra.addFlashAttribute("successAdd", "Kryptowährungskonten sind in Kürze verfügbar.");
        return "redirect:/konto";
    }

    // POST: editar/excluir (Bank + Custom)
    @PostMapping("/konto/bearbeiten")
    public String bearbeiten(HttpSession session,
                             // BANK
                             @RequestParam(value = "bank_id", required = false) List<Integer> bankIds,
                             @RequestParam(value = "bankname", required = false) List<String> banknames,
                             @RequestParam(value = "iban", required = false) List<String> ibans,
                             @RequestParam(value = "bic", required = false) List<String> bics,
                             @RequestParam(value = "delete_bank", required = false) List<Integer> deleteBankIds,
                             // CUSTOM
                             @RequestParam(value = "custom_id", required = false) List<Integer> customIds,
                             @RequestParam(value = "c_name", required = false) List<String> cNames,
                             @RequestParam(value = "c_beschreibung", required = false) List<String> cBeschreibungen,
                             @RequestParam(value = "delete_custom", required = false) List<Integer> deleteCustomIds,
                             RedirectAttributes ra) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        if (bankIds == null) bankIds = new ArrayList<>();
        if (banknames == null) banknames = new ArrayList<>();
        if (ibans == null) ibans = new ArrayList<>();
        if (bics == null) bics = new ArrayList<>();
        if (deleteBankIds == null) deleteBankIds = new ArrayList<>();

        if (customIds == null) customIds = new ArrayList<>();
        if (cNames == null) cNames = new ArrayList<>();
        if (cBeschreibungen == null) cBeschreibungen = new ArrayList<>();
        if (deleteCustomIds == null) deleteCustomIds = new ArrayList<>();

        // BANK
        for (int i = 0; i < bankIds.size(); i++) {
            Integer id = bankIds.get(i);
            if (id == null) continue;

            if (deleteBankIds.contains(id)) {
                try { bankkontoDAO.deleteById(id); } catch (SQLException e) { e.printStackTrace(); }
                continue;
            }

            String name = i < banknames.size() ? banknames.get(i) : "";
            String iban = i < ibans.size() ? ibans.get(i) : "";
            String bic  = i < bics.size() ? bics.get(i) : "";

            try {
                bankkontoDAO.updateStammdaten(
                        id,
                        name == null ? "" : name.trim(),
                        iban == null ? "" : iban.trim(),
                        bic  == null ? "" : bic.trim()
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // CUSTOM
        for (int i = 0; i < customIds.size(); i++) {
            Integer id = customIds.get(i);
            if (id == null) continue;

            if (deleteCustomIds.contains(id)) {
                try { bankkontoDAO.deleteById(id); } catch (SQLException e) { e.printStackTrace(); }
                continue;
            }

            String name = i < cNames.size() ? cNames.get(i) : "";
            String beschr = i < cBeschreibungen.size() ? cBeschreibungen.get(i) : "";
            String combined = (name == null ? "" : name.trim());
            if (beschr != null && !beschr.trim().isEmpty()) {
                combined = combined + " (" + beschr.trim() + ")";
            }

            try {
                bankkontoDAO.updateStammdaten(id, combined, "", "");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        ra.addFlashAttribute("successEdit", "Konten wurden erfolgreich aktualisiert.");

        return "redirect:/konto";
    }
}
