package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.NutzerDAO;
import com.cashflow.app.model.Nutzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class NutzerControllerMockitoTest {

    @Mock
    private NutzerDAO nutzerDAO;

    @InjectMocks
    private NutzerController nutzerController;

    @Test
    void sollteGueltigenLoginAkzeptieren() throws SQLException {
        // Vorbereitung
        Nutzer nutzer = new Nutzer(
                1,
                "Max",
                "Mustermann",
                "max@example.com",
                "geheim"
        );

        when(nutzerDAO.findByEmail("max@example.com"))
                .thenReturn(nutzer);

        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        // Ausführung
        String viewName = nutzerController.loginNutzer(
                "max@example.com",
                "geheim",
                model,
                session
        );

        // Prüfung
        assertEquals("redirect:/home", viewName);
        assertSame(
                nutzer,
                session.getAttribute("eingeloggterNutzer")
        );

        verify(nutzerDAO).findByEmail("max@example.com");
    }

    @Test
    void sollteLoginMitFalschemPasswortAblehnen() throws SQLException {
        // Vorbereitung
        Nutzer nutzer = new Nutzer(
                1,
                "Max",
                "Mustermann",
                "max@example.com",
                "geheim"
        );

        when(nutzerDAO.findByEmail("max@example.com"))
                .thenReturn(nutzer);

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        // Ausführung
        String viewName = nutzerController.loginNutzer(
                "max@example.com",
                "falschesPasswort",
                model,
                session
        );

        // Prüfung
        assertEquals("nutzer-login-und-form", viewName);
        assertEquals(
                "E-Mail oder Passwort ist ungültig.",
                model.get("errorMessage")
        );
        assertNull(session.getAttribute("eingeloggterNutzer"));

        verify(nutzerDAO).findByEmail("max@example.com");
    }

    @Test
    void sollteDatenbankfehlerBeimLoginBehandeln() throws SQLException {
        // Vorbereitung
        when(nutzerDAO.findByEmail("max@example.com"))
                .thenThrow(new SQLException("Datenbank nicht erreichbar"));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        // Ausführung
        String viewName = nutzerController.loginNutzer(
                "max@example.com",
                "geheim",
                model,
                session
        );

        // Prüfung
        assertEquals("nutzer-login-und-form", viewName);
        assertEquals(
                "Fehler beim Zugriff auf die Datenbank.",
                model.get("errorMessage")
        );
        assertNull(session.getAttribute("eingeloggterNutzer"));

        verify(nutzerDAO).findByEmail("max@example.com");
    }
}