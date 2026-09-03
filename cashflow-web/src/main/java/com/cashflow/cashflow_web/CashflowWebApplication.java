package com.cashflow.cashflow_web;

import com.cashflow.app.dao.NutzerDAO;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.cashflow.app.dao.BankkontoDAO;

@SpringBootApplication
public class CashflowWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(CashflowWebApplication.class, args);
	}

	@Bean
	public NutzerDAO nutzerDAO() {
		return new NutzerDAO();
	}

	@Bean
	public BankkontoDAO bankkontoDAO() {
		return new BankkontoDAO();
	}
}