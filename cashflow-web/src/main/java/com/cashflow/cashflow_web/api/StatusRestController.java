package com.cashflow.cashflow_web.api;

import com.cashflow.cashflow_web.api.dto.StatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatusRestController {

    @GetMapping("/status")
    public StatusResponse getStatus() {
        return new StatusResponse("UP", "CashFlow");
    }
}