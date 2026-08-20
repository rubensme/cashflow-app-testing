package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.GeplanteTransaktionDAO;
import com.cashflow.app.model.GeplanteTransaktion;
import com.cashflow.app.model.Nutzer;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class GeplanteTransaktionController {

    private final GeplanteTransaktionDAO geplanteDAO = new GeplanteTransaktionDAO();

    // Lista (ainda disponível se você quiser acessar a página própria)
    @GetMapping("/geplante")
    public String liste(Model model, HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        List<GeplanteTransaktion> list = geplanteDAO.findByNutzerId(n.getNutzerId());

        // adiciona uma linha vazia para novo cadastro (com defaults corretos)
        List<GeplanteTransaktion> edit = new ArrayList<>(list);
        GeplanteTransaktion empty = new GeplanteTransaktion();
        empty.setGeplanteId(0);
        empty.setNutzerId(n.getNutzerId());
        empty.setTitel("");
        empty.setBetrag(0.0);
        empty.setPeriode("MONATLICH");   // default correto
        empty.setStatus("AKTIV");        // default mais coerente
        empty.setStartDatum(LocalDate.now());
        edit.add(empty);

        model.addAttribute("geplante", edit);
        return "geplante"; // Thymeleaf template (opcional)
    }

    // POST em lote (tabela editável). Agora redireciona para /home.
    @PostMapping("/geplante/uebernehmen")
    public String uebernehmen(HttpSession session,
                              @RequestParam("id") List<Integer> ids,
                              @RequestParam("titel") List<String> titel,
                              @RequestParam("datum") List<String> daten,
                              @RequestParam("betrag") List<Double> betraege,
                              @RequestParam("periode") List<String> perioden,
                              @RequestParam("status") List<String> status,
                              @RequestParam(value = "delete", required = false) List<Integer> deleteFlags) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";
        int nutzerId = n.getNutzerId();

        for (int i = 0; i < ids.size(); i++) {
            int id = safeInt(ids.get(i));
            String t = safeString(titel.get(i));
            String d = safeString(daten.get(i));
            Double b = betraege.get(i) != null ? betraege.get(i) : 0.0;
            String p = normalizePeriode(perioden.get(i));
            String s = normalizeStatus(status.get(i));

            boolean markedDelete = (deleteFlags != null && deleteFlags.contains(i));
            if (id > 0 && markedDelete) {
                geplanteDAO.deleteById(id);
                continue;
            }
            // ignorar linha “nova” totalmente vazia
            if (id == 0 && t.isEmpty()) continue;

            GeplanteTransaktion g = new GeplanteTransaktion();
            g.setGeplanteId(id);
            g.setNutzerId(nutzerId);
            g.setTitel(t);
            g.setStartDatum(parseDateOrToday(d));
            g.setBetrag(b);
            g.setPeriode(p);   // já normalizado
            g.setStatus(s);    // já normalizado

            if (id > 0) {
                geplanteDAO.update(g);
            } else {
                try {
                    geplanteDAO.save(g);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "redirect:/home";
    }

    // POST para o formulário da HOME (objeto único: "geplanteForm")
    @PostMapping("/geplante/speichern")
    public String speichernVonHome(@ModelAttribute("geplanteForm") GeplanteTransaktion g,
                                   HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";

        // garantir vínculo e normalizações
        g.setNutzerId(n.getNutzerId());
        g.setTitel(safeString(g.getTitel()));
        g.setPeriode(normalizePeriode(g.getPeriode())); // MONATLICH/EINMALIG/...
        g.setStatus(normalizeStatus(g.getStatus()));
        if (g.getStartDatum() == null) g.setStartDatum(LocalDate.now());

        if (g.getGeplanteId() > 0 && g.getGeplanteId() > 0) {
            geplanteDAO.update(g);
        } else {
            try {
                geplanteDAO.save(g);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "redirect:/home";
    }

    // ==== Helpers locais (consistentes com o DAO) ====
    private static String safeString(String s) {
        return s == null ? "" : s.trim();
    }

    private static int safeInt(Integer i) {
        return i == null ? 0 : i;
    }

    private static LocalDate parseDateOrToday(String s) {
        try {
            String x = safeString(s);
            if (x.length() >= 10) x = x.substring(0, 10);
            return x.isEmpty() ? LocalDate.now() : LocalDate.parse(x);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private static String normalizePeriode(String p) {
        if (p == null || p.trim().isEmpty()) return "MONATLICH";
        String x = p.trim().toUpperCase(Locale.ROOT);
        if (x.startsWith("EINM")) return "EINMALIG";
        if (x.startsWith("MONAT")) return "MONATLICH";
        if (x.startsWith("WOECH") || x.startsWith("WÖCH")) return "WOECHENTLICH";
        if (x.startsWith("VIERTEL")) return "VIERTELJAEHRLICH";
        if (x.startsWith("JAEHR") || x.startsWith("JÄHR")) return "JAEHRLICH";
        return "EINMALIG";
        }

    private static String normalizeStatus(String s) {
        if (s == null || s.trim().isEmpty()) return "AKTIV";
        String x = s.trim().toUpperCase(Locale.ROOT);
        if (x.startsWith("AKT")) return "AKTIV";
        if (x.startsWith("BEZ")) return "BEZAHLT";
        if (x.startsWith("OFF")) return "AKTIV"; // migração de rótulo antigo
        return "AKTIV";
    }
}
