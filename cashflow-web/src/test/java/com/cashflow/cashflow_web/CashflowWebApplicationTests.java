package com.cashflow.cashflow_web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@AutoConfigureMockMvc
class CashflowWebApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void sollteNutzerSeiteErfolgreichLaden() throws Exception {

		mockMvc.perform(get("/nutzer"))
				.andExpect(status().isOk())
				.andExpect(view().name("nutzer-login-und-form"))
				.andExpect(model().attributeExists("nutzer"));
	}

	@Test
	void sollteRegistrierungMitLeerenFeldernAblehnen() throws Exception {

		mockMvc.perform(post("/nutzer")
						.param("vorname", "")
						.param("nachname", "")
						.param("email", "")
						.param("passwort", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("nutzer-login-und-form"))
				.andExpect(model().attribute(
						"errorMessage",
						"Bitte füllen Sie alle Felder aus."
				));
	}
}