package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.dao.CashTransaktionDAO;
import com.cashflow.app.dao.NutzerEinstellungenDAO;
import com.cashflow.app.dao.TransaktionDAO;
import com.cashflow.app.dao.GeplanteTransaktionDAO;

import com.cashflow.app.dto.GeplanteOccurrence;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Nutzer;
import com.cashflow.app.model.Transaktion;
import com.cashflow.app.model.GeplanteTransaktion;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final BankkontoDAO bankkontoDAO = new BankkontoDAO();
    private final TransaktionDAO transaktionDAO = new TransaktionDAO();
    private final CashTransaktionDAO cashTransaktionDAO = new CashTransaktionDAO();
    private final NutzerEinstellungenDAO einstellungenDAO = new NutzerEinstellungenDAO();
    private final GeplanteTransaktionDAO geplanteDAO = new GeplanteTransaktionDAO();

    @GetMapping("/home")
    public String home(
            Model model,
            HttpSession session,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(name = "suche", required = false) Boolean suche,
            @RequestParam(name = "von", required = false) String von,
            @RequestParam(name = "bis", required = false) String bis,
            @RequestParam(name = "prognoseBis", required = false) String prognoseBis,
            @RequestParam(name = "editGeplante", required = false) Boolean editGeplante
    ) throws SQLException {

        Nutzer nutzer = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (nutzer == null) return "home";
        int nutzerId = nutzer.getNutzerId();

        // ==== Preferência: incluir Bargeld (Cash no backend)? ====
        boolean includeCash = einstellungenDAO.getByNutzerId(nutzerId).isIncludeCash();
        model.addAttribute("includeCash", includeCash);

        // ==== Contas do usuário ====
        List<Bankkonto> bankkonten;
        try {
            bankkonten = bankkontoDAO.findByNutzerId(nutzerId);
        } catch (SQLException e) {
            e.printStackTrace();
            bankkonten = Collections.emptyList();
        }
        model.addAttribute("bankkonten", bankkonten);

        // ==== IDs marcados ====
        Set<Integer> checkedIds = bankkontoDAO.findCheckedKontoIdsByNutzerId(nutzerId);
        final Set<Integer> checkedIdsFinal = (checkedIds == null) ? Collections.emptySet() : checkedIds;
        model.addAttribute("checkedIds", checkedIdsFinal);

        // ==== Saldos ====
        double saldoBankkontenMarcados = bankkonten.stream()
                .filter(k -> checkedIdsFinal.contains(k.getBankKontoId()))
                .mapToDouble(Bankkonto::getAktuellerSaldo)
                .sum();

        double cashSaldoActual = cashTransaktionDAO.sumCashSaldoByNutzerId(nutzerId); // sempre calcula
        if (Math.abs(cashSaldoActual) < 0.0005) cashSaldoActual = 0.0; // evita -0,00
        model.addAttribute("saldoCash", cashSaldoActual);               // exibe mesmo se não marcado

        double gesamtsaldo = saldoBankkontenMarcados + (includeCash ? cashSaldoActual : 0.0);
        if (Math.abs(gesamtsaldo) < 0.0005) gesamtsaldo = 0.0;          // evita -0,00
        model.addAttribute("gesamtsaldo", String.format("%.2f", gesamtsaldo));

        // ==== Tabela principal (últimas N ou busca) ====
        model.addAttribute("limitAtual", limit);

        boolean suchModus = (suche != null && suche && von != null && !von.isBlank());
        model.addAttribute("suche", suche != null && suche);
        model.addAttribute("suchModus", suchModus);
        model.addAttribute("von", von);
        model.addAttribute("bis", bis);

        List<Transaktion> transacoes;

        if (suchModus) {
            // BUSCA POR DATA
            LocalDate vonDate = LocalDate.parse(von);
            LocalDate bisDate = (bis == null || bis.isBlank()) ? vonDate : LocalDate.parse(bis);
            if (bisDate.isBefore(vonDate)) { LocalDate tmp = vonDate; vonDate = bisDate; bisDate = tmp; }

            if (!checkedIdsFinal.isEmpty()) {
                transacoes = transaktionDAO.findByKontenIdsBetweenDates(checkedIdsFinal, vonDate, bisDate);
            } else {
                if (includeCash) {
                    transacoes = transaktionDAO.findByNutzerIdBetweenDates(nutzerId, vonDate, bisDate);
                    if (transacoes != null) {
                        transacoes = transacoes.stream()
                                .filter(t -> "Cash".equalsIgnoreCase(t.getQuelle()))
                                .collect(Collectors.toList());
                    }
                } else {
                    transacoes = Collections.emptyList();
                }
            }

        } else {
            // MODO NORMAL (últimas N)
            if (!checkedIdsFinal.isEmpty()) {
                transacoes = transaktionDAO.findLastByKontenIdsOrderByDatumDesc(checkedIdsFinal, limit);
            } else {
                if (includeCash) {
                    transacoes = transaktionDAO.findLast10ByNutzerIdOrderByDatumDesc(nutzerId, limit);
                    if (transacoes != null) {
                        transacoes = transacoes.stream()
                                .filter(t -> "Cash".equalsIgnoreCase(t.getQuelle()))
                                .collect(Collectors.toList());
                    }
                } else {
                    transacoes = Collections.emptyList();
                }
            }
        }

        // Se includeCash=false, remover Bargeld da lista
        if (!includeCash && transacoes != null) {
            transacoes = transacoes.stream()
                    .filter(t -> !"Cash".equalsIgnoreCase(t.getQuelle()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("letzteTransaktionen", transacoes == null ? Collections.emptyList() : transacoes);
        model.addAttribute("nutzer", nutzer);

        // ==== Série do saldo: últimos 30 dias ====
        LocalDate heute = LocalDate.now();
        LocalDate start = heute.minusDays(29);

        // 1) Busca transações da janela "teórica" de 30 dias
        List<Transaktion> tx30;
        if (!checkedIdsFinal.isEmpty()) {
            tx30 = transaktionDAO.findByKontenIdsBetweenDates(checkedIdsFinal, start, heute);
            if (!includeCash && tx30 != null) {
                tx30 = tx30.stream()
                        .filter(t -> !"Cash".equalsIgnoreCase(t.getQuelle()))
                        .collect(Collectors.toList());
            }
        } else {
            if (includeCash) {
                tx30 = transaktionDAO.findByNutzerIdBetweenDates(nutzerId, start, heute);
                if (tx30 != null) {
                    tx30 = tx30.stream()
                            .filter(t -> "Cash".equalsIgnoreCase(t.getQuelle()))
                            .collect(Collectors.toList());
                }
            } else {
                tx30 = Collections.emptyList();
            }
        }

        // 2) Agrega por dia
        Map<LocalDate, Double> sumPorDia = new HashMap<>();
        if (tx30 != null) {
            for (Transaktion t : tx30) {
                if (t.getDatum() == null) continue;
                sumPorDia.merge(t.getDatum(), t.getBetrag(), Double::sum);
            }
        }

        // 3) Se não há dados -> arrays vazios (gráfico não desenha)
        if (sumPorDia.isEmpty()) {
            model.addAttribute("saldo30Labels", Collections.emptyList());
            model.addAttribute("saldo30Values", Collections.emptyList());
        } else {
            // 4) Começa no primeiro dia com dado OU (hoje-29)
            LocalDate minDate = Collections.min(sumPorDia.keySet());
            LocalDate finalStart = minDate.isAfter(start) ? minDate : start;

            // 5) Base = saldo atual - somatório da janela [finalStart..hoje]
            double somaJanela = sumPorDia.entrySet().stream()
                    .filter(e -> !e.getKey().isBefore(finalStart))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            double base = gesamtsaldo - somaJanela;

            // 6) Construção da série diária
            DateTimeFormatter labFmt = DateTimeFormatter.ofPattern("dd.MM");
            List<String> saldo30Labels = new ArrayList<>();
            List<Double> saldo30Values = new ArrayList<>();
            double running = base;

            for (LocalDate d = finalStart; !d.isAfter(heute); d = d.plusDays(1)) {
                running += sumPorDia.getOrDefault(d, 0.0);
                double val = Math.abs(running) < 0.0005 ? 0.0 : Math.round(running * 100.0) / 100.0;
                saldo30Labels.add(d.format(labFmt));
                saldo30Values.add(val);
            }

            model.addAttribute("saldo30Labels", saldo30Labels);
            model.addAttribute("saldo30Values", saldo30Values);
        }

        // ==== Geplante Transaktionen na HOME ====
        List<GeplanteTransaktion> geplanteList = geplanteDAO.findByNutzerId(nutzerId);
        model.addAttribute("geplanteTransaktionen", geplanteList);
        model.addAttribute("editGeplante", editGeplante != null && editGeplante);

        // Form padrão com periode default = MONATLICH
        GeplanteTransaktion geplanteForm = new GeplanteTransaktion();
        geplanteForm.setNutzerId(nutzerId);
        geplanteForm.setTitel("");
        geplanteForm.setStartDatum(LocalDate.now());
        geplanteForm.setBetrag(0.00);
        geplanteForm.setPeriode("MONATLICH");
        geplanteForm.setStatus("AKTIV");
        model.addAttribute("geplanteForm", geplanteForm);

        // ==== Finanzprognose ====
        boolean hatGeplante = !geplanteList.isEmpty();
        model.addAttribute("hatGeplante", hatGeplante);

        LocalDate defaultEnd = YearMonth.now().atEndOfMonth();
        LocalDate ziel = (prognoseBis == null || prognoseBis.isBlank()) ? defaultEnd : LocalDate.parse(prognoseBis);

        List<GeplanteOccurrence> vorgaenge = geplanteDAO.findOccurrencesUpTo(nutzerId, heute, ziel);
        double sumVorgaenge = vorgaenge.stream().mapToDouble(GeplanteOccurrence::getBetrag).sum();
        double prognoseSaldo = gesamtsaldo + sumVorgaenge;
        if (Math.abs(prognoseSaldo) < 0.0005) prognoseSaldo = 0.0;

        model.addAttribute("prognoseBis", ziel);
        model.addAttribute("prognoseVorgaenge", vorgaenge);
        model.addAttribute("prognoseSaldo", String.format("%.2f", prognoseSaldo));

        return "home";
    }

    // ==== POST: Atualizar seleção de contas + includeCash ====
    @PostMapping("/home/updateCheckedKonten")
    public String updateCheckedKonten(
            HttpSession session,
            @RequestParam(value = "checkedKonten", required = false) List<Integer> checkedIdsForm,
            @RequestParam(value = "includeCash", required = false) String includeCashParam
    ) throws SQLException {
        Nutzer nutzer = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (nutzer == null) return "redirect:/nutzer";

        int nutzerId = nutzer.getNutzerId();

        Set<Integer> checkedIds = new HashSet<>();
        if (checkedIdsForm != null) checkedIds.addAll(checkedIdsForm);

        try {
            bankkontoDAO.updateCheckedByIds(nutzerId, checkedIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        boolean includeCash = (includeCashParam != null);
        einstellungenDAO.updateIncludeCash(nutzerId, includeCash);

        return "redirect:/home";
    }
}
