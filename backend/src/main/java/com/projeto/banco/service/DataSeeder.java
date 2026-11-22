package com.projeto.banco.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CupomClienteService cupomClienteService;

    public DataSeeder(CupomClienteService cupomClienteService) {
        this.cupomClienteService = cupomClienteService;
    }

    @Override
    public void run(String... args) {
        cupomClienteService.garantirSeeds();
    }
}
