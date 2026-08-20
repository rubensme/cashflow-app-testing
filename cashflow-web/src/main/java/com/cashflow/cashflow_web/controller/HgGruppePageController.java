package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.dao.HaushaltsgruppeDAO;
import com.cashflow.app.dao.GeplanteTransaktionDAO;
import com.cashflow.app.dto.GeplanteOccurrence;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Haushaltsgruppe;
import com.cashflow.app.model.HaushaltsgruppeMitglied;
import com.cashflow.app.model.Nutzer;
import com.cashflow.app.model.Transaktion;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HgGruppePageController {

    private final HaushaltsgruppeDAO gruppeDAO = new HaushaltsgruppeDAO();
    private final BankkontoDAO bankkontoDAO = new BankkontoDAO();
    private final GeplanteTransaktionDAO geplanteDAO = new GeplanteTransaktionDAO();

    @GetMapping("/hg/gruppe/{id}")
    public String gruppeSeite(@PathVariable("id") int id,
                              @RequestParam(value = "prognoseBis", required = false) String prognoseBisStr,
                              Model model,
                              HttpSession session,
                              RedirectAttributes ra) throws SQLException {

        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";
        final int userId = n.getNutzerId();

        Haushaltsgruppe g = gruppeDAO.findById(id);
        if (g == null) {
            ra.addFlashAttribute("hgError", "Gruppe nicht gefunden.");
            return "redirect:/haushaltsgruppe";
        }

        List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(id);
        boolean isOwner = g.getOwnerNutzerId() == userId;
        boolean isAktiv = members != null && members.stream()
                .anyMatch(m -> m.getNutzerId() == userId && m.isAktiv());
        if (!isOwner && !isAktiv) {
            ra.addFlashAttribute("hgError", "Kein Zugriff auf diese Gruppe.");
            return "redirect:/haushaltsgruppe";
        }

        // Navbar
        List<Haushaltsgruppe> owned = gruppeDAO.findOwnedBy(userId);
        List<Haushaltsgruppe> member = gruppeDAO.findMemberOfButNotOwner(userId);
        owned.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        member.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        model.addAttribute("ownedGroups", owned);
        model.addAttribute("memberGroups", member);

        // Blocos (GET: tudo pré-selecionado)
        List<Map<String, Object>> mitglieder = buildMitgliederBlocks(g, members, Collections.emptySet(), false);

        // IDs de todas as contas visíveis
        Set<Integer> allKontoIds = mitglieder.stream()
                .flatMap(m -> ((List<Map<String,Object>>) m.get("konten")).stream())
                .map(r -> (Integer) r.get("kontoId"))
                .collect(Collectors.toSet());

        // Gesamtsaldo inicial = soma de todos os membros (pré-marcado)
        double gruppeSaldoInitial = mitglieder.stream()
                .mapToDouble(m -> (double) m.getOrDefault("total", 0.0))
                .sum();

        // Série do gráfico com todas as contas
        Series serie = computeGroupSaldoSeries(allKontoIds, members);

        // ===== Geplante (por membro) =====
        LocalDate heute = LocalDate.now();
        LocalDate prognoseBis = parseDateOrDefault(prognoseBisStr, heute.plusDays(30));

        List<Map<String, Object>> prognoseRows = buildPrognoseRows(members, heute, prognoseBis);
        double sumOccurrences = prognoseRows.stream()
                .mapToDouble(r -> (double) r.getOrDefault("betrag", 0.0))
                .sum();
        double prognostiziertesSaldo = gruppeSaldoInitial + sumOccurrences;

        // Model
        model.addAttribute("gruppe", g);
        model.addAttribute("mitglieder", mitglieder);
        model.addAttribute("gruppeSaldo", gruppeSaldoInitial);
        model.addAttribute("checkedKontoIds", allKontoIds);

        model.addAttribute("saldo30Labels", serie.labels);
        model.addAttribute("saldo30Values", serie.values);

        model.addAttribute("prognoseBis", prognoseBis);
        model.addAttribute("prognoseVorgaengeMitglied", prognoseRows);
        model.addAttribute("hatGeplante", !prognoseRows.isEmpty());
        model.addAttribute("prognostiziertesSaldo", prognostiziertesSaldo);

        return "hg_gruppe";
    }
    
    

    @PostMapping("/hg/gruppe/{id}/uebernehmen")
    public String uebernehmen(@PathVariable("id") int id,
                              @RequestParam(value = "kontoIds", required = false) List<Integer> kontoIds,
                              Model model,
                              HttpSession session,
                              RedirectAttributes ra) throws SQLException {

        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return "redirect:/nutzer";
        final int userId = n.getNutzerId();

        Haushaltsgruppe g = gruppeDAO.findById(id);
        if (g == null) {
            ra.addFlashAttribute("hgError", "Gruppe nicht gefunden.");
            return "redirect:/haushaltsgruppe";
        }

        List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(id);
        boolean isOwner = g.getOwnerNutzerId() == userId;
        boolean isAktiv = members != null && members.stream()
                .anyMatch(m -> m.getNutzerId() == userId && m.isAktiv());
        if (!isOwner && !isAktiv) {
            ra.addFlashAttribute("hgError", "Kein Zugriff auf diese Gruppe.");
            return "redirect:/haushaltsgruppe";
        }

        Set<Integer> selected = (kontoIds == null) ? Collections.emptySet() : new HashSet<>(kontoIds);

        // Blocos por Mitglied + soma apenas dos selecionados
        List<Map<String, Object>> mitglieder = buildMitgliederBlocks(g, members, selected, true);
        double gruppeSaldo = mitglieder.stream()
                .mapToDouble(m -> (double) m.getOrDefault("selectedSum", 0.0))
                .sum();

        // Série do gráfico baseada nas contas selecionadas
        Series serie = computeGroupSaldoSeries(selected, members);

        // Geplante (usar padrão de 30 dias à frente)
        LocalDate heute = LocalDate.now();
        LocalDate prognoseBis = heute.plusDays(30);
        List<Map<String, Object>> prognoseRows = buildPrognoseRows(members, heute, prognoseBis);
        double sumOccurrences = prognoseRows.stream()
                .mapToDouble(r -> (double) r.getOrDefault("betrag", 0.0))
                .sum();
        double prognostiziertesSaldo = gruppeSaldo + sumOccurrences;

        // Navbar
        List<Haushaltsgruppe> owned = gruppeDAO.findOwnedBy(userId);
        List<Haushaltsgruppe> member = gruppeDAO.findMemberOfButNotOwner(userId);
        owned.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        member.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        model.addAttribute("ownedGroups", owned);
        model.addAttribute("memberGroups", member);

        // Model
        model.addAttribute("gruppe", g);
        model.addAttribute("mitglieder", mitglieder);
        model.addAttribute("gruppeSaldo", gruppeSaldo);
        model.addAttribute("checkedKontoIds", selected);

        model.addAttribute("saldo30Labels", serie.labels);
        model.addAttribute("saldo30Values", serie.values);

        model.addAttribute("prognoseBis", prognoseBis);
        model.addAttribute("prognoseVorgaengeMitglied", prognoseRows);
        model.addAttribute("hatGeplante", !prognoseRows.isEmpty());
        model.addAttribute("prognostiziertesSaldo", prognostiziertesSaldo);

        return "hg_gruppe";
    }

    private List<Map<String, Object>> buildMitgliederBlocks(Haushaltsgruppe g,
                                                            List<HaushaltsgruppeMitglied> members,
                                                            Set<Integer> selectedKontoIds,
                                                            boolean computeSelectedSum) {
        List<Map<String, Object>> mitglieder = new ArrayList<>();
        if (members == null) return mitglieder;

        members.sort((a, b) -> {
            if (a.getNutzerId() == g.getOwnerNutzerId() && b.getNutzerId() != g.getOwnerNutzerId()) return -1;
            if (b.getNutzerId() == g.getOwnerNutzerId() && a.getNutzerId() != g.getOwnerNutzerId()) return 1;
            return Integer.compare(a.getNutzerId(), b.getNutzerId());
        });

        for (HaushaltsgruppeMitglied m : members) {
            Map<String, Object> block = new HashMap<>();
            block.put("vorname", safe(m.getVorname()));
            block.put("email", safe(m.getEmail()));
            block.put("nutzerId", m.getNutzerId());
            block.put("isOwner", m.getNutzerId() == g.getOwnerNutzerId());
            block.put("isPending", !m.isAktiv());

            double mitgliedTotal = 0.0;
            double selectedSum = 0.0;
            List<Map<String, Object>> krows = new ArrayList<>();

            if (m.isAktiv()) {
                List<Bankkonto> konten;
                try {
                    konten = bankkontoDAO.findByNutzerId(m.getNutzerId());
                } catch (Exception e) {
                    konten = Collections.emptyList();
                }

                for (Bankkonto k : konten) {
                    double saldo = 0.0;
                    try { saldo = k.getAktuellerSaldo(); } catch (Exception ignored) {}

                    Map<String, Object> row = new HashMap<>();
                    row.put("kontoId", k.getBankKontoId());
                    row.put("kontoName", safe(k.getNameDerBank()));
                    row.put("saldo", saldo);
                    krows.add(row);

                    mitgliedTotal += saldo;
                    if (computeSelectedSum) {
                        if (selectedKontoIds.contains(k.getBankKontoId())) selectedSum += saldo;
                    } else {
                        selectedSum += saldo; // GET: tudo pré-selecionado
                    }
                }
            }

            block.put("konten", krows);
            block.put("total", mitgliedTotal);
            block.put("selectedSum", selectedSum);

            mitglieder.add(block);
        }

        return mitglieder;
    }

    /** Série diária: inicia na 1ª transação real; se não houver, um único ponto (hoje). */
    private Series computeGroupSaldoSeries(Set<Integer> selectedKontoIds,
                                           List<HaushaltsgruppeMitglied> members) {

        Series s = new Series();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        if (members == null || selectedKontoIds == null || selectedKontoIds.isEmpty()) {
            s.labels = labels;
            s.values = values;
            return s;
        }

        LocalDate earliest = null;
        LocalDate today = LocalDate.now();
        double currentTotal = 0.0;
        Map<LocalDate, Double> deltaByDate = new HashMap<>();

        for (HaushaltsgruppeMitglied m : members) {
            if (!m.isAktiv()) continue;

            List<Bankkonto> konten;
            try {
                konten = bankkontoDAO.findByNutzerId(m.getNutzerId());
            } catch (Exception e) {
                konten = Collections.emptyList();
            }

            for (Bankkonto k : konten) {
                if (!selectedKontoIds.contains(k.getBankKontoId())) continue;

                try { currentTotal += k.getAktuellerSaldo(); } catch (Exception ignored) {}

                List<Transaktion> txs = k.getTransaktionen();
                if (txs == null) continue;

                for (Transaktion t : txs) {
                    LocalDate d = t.getDatum();
                    if (d == null) continue;
                    earliest = (earliest == null || d.isBefore(earliest)) ? d : earliest;
                    deltaByDate.merge(d, t.getBetrag(), Double::sum);
                }
            }
        }

        if (earliest == null) {
            labels.add(today.format(DateTimeFormatter.ofPattern("dd.MM")));
            values.add(currentTotal);
            s.labels = labels;
            s.values = values;
            return s;
        }

        int days = (int) (today.toEpochDay() - earliest.toEpochDay()) + 1;
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM");

        double[] dailyDelta = new double[days];
        double sumDelta = 0.0;
        for (int i = 0; i < days; i++) {
            LocalDate d = earliest.plusDays(i);
            double v = deltaByDate.getOrDefault(d, 0.0);
            dailyDelta[i] = v;
            sumDelta += v;
            labels.add(df.format(d));
        }

        double base = currentTotal - sumDelta;
        double running = base;
        for (int i = 0; i < days; i++) {
            running += dailyDelta[i];
            values.add(running);
        }

        s.labels = labels;
        s.values = values;
        return s;
    }

    private List<Map<String, Object>> buildPrognoseRows(List<HaushaltsgruppeMitglied> members,
                                                        LocalDate from, LocalDate bis) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (members == null || members.isEmpty()) return rows;

        Map<Integer, String> nameById = new HashMap<>();
        Set<Integer> activeIds = new HashSet<>();
        for (HaushaltsgruppeMitglied m : members) {
            if (!m.isAktiv()) continue;
            activeIds.add(m.getNutzerId());
            String name = (safe(m.getVorname()).isEmpty() && safe(m.getEmail()).isEmpty())
                    ? ("Nutzer " + m.getNutzerId())
                    : (safe(m.getVorname()).isEmpty() ? m.getEmail() : m.getVorname());
            nameById.put(m.getNutzerId(), name);
        }
        if (activeIds.isEmpty()) return rows;

        Map<Integer, List<GeplanteOccurrence>> occByUser =
                geplanteDAO.findOccurrencesUpToByNutzerIds(activeIds, from, bis);

        for (Map.Entry<Integer, List<GeplanteOccurrence>> e : occByUser.entrySet()) {
            String mitglied = nameById.getOrDefault(e.getKey(), "Mitglied");
            for (GeplanteOccurrence o : e.getValue()) {
                Map<String, Object> r = new HashMap<>();
                r.put("datum", o.getDatum());
                r.put("mitglied", mitglied);
                r.put("titel", o.getTitel());
                r.put("betrag", o.getBetrag());
                rows.add(r);
            }
        }

        rows.sort(Comparator.comparing(a -> (LocalDate) a.get("datum")));
        return rows;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static LocalDate parseDateOrDefault(String s, LocalDate def) {
        try {
            if (s == null || s.isBlank()) return def;
            return LocalDate.parse(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static class Series {
        List<String> labels;
        List<Double> values;
    }
}
