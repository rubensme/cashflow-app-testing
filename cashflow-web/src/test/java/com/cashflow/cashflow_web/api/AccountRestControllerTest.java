package com.cashflow.cashflow_web.api;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Nutzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountRestController.class)
class AccountRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankkontoDAO bankkontoDAO;

    @Test
    void sollteNichtAngemeldetenZugriffAblehnen() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bankkontoDAO);
    }

    @Test
    void sollteKontenDesAngemeldetenNutzersAlsJsonZurueckgeben()
            throws Exception {

        Nutzer nutzer = new Nutzer(
                1,
                "Max",
                "Mustermann",
                "max@example.com",
                "geheim"
        );

        Bankkonto konto = new Bankkonto();
        konto.setBankKontoId(10);
        konto.setNameDerBank("Sparkasse");
        konto.setAktuellerSaldo(250.75);
        konto.setChecked(true);

        when(bankkontoDAO.findByNutzerId(1))
                .thenReturn(List.of(konto));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("eingeloggterNutzer", nutzer);

        mockMvc.perform(
                        get("/api/accounts")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].accountId").value(10))
                .andExpect(jsonPath("$[0].name").value("Sparkasse"))
                .andExpect(jsonPath("$[0].balance").value(250.75))
                .andExpect(jsonPath("$[0].selected").value(true));

        verify(bankkontoDAO).findByNutzerId(1);
    }

    @Test
    void sollteBeiDatenbankfehlerStatus500Zurueckgeben()
            throws Exception {

        Nutzer nutzer = new Nutzer(
                1,
                "Max",
                "Mustermann",
                "max@example.com",
                "geheim"
        );

        when(bankkontoDAO.findByNutzerId(1))
                .thenThrow(new SQLException("Datenbank nicht erreichbar"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("eingeloggterNutzer", nutzer);

        mockMvc.perform(
                        get("/api/accounts")
                                .session(session)
                )
                .andExpect(status().isInternalServerError());

        verify(bankkontoDAO).findByNutzerId(1);
    }
}