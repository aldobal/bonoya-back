package com.bonoya.platform.bonos.application.services;

import com.bonoya.platform.bonos.domain.model.entities.Calculo;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.CalculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CalculoService {

    private final CalculoRepository calculoRepository;

    @Autowired
    public CalculoService(CalculoRepository calculoRepository) {
        this.calculoRepository = calculoRepository;
    }

    public Calculo guardarCalculo(Calculo calculo) {
        return calculoRepository.save(calculo);
    }

    public Optional<Calculo> obtenerCalculoPorId(Long id) {
        return calculoRepository.findById(id);
    }

    public List<Calculo> obtenerCalculosPorInversor(String inversorUsername) {
        return calculoRepository.findByInversorUsername(inversorUsername);
    }

    public List<Calculo> obtenerCalculosPorBono(Long bonoId) {
        return calculoRepository.findByBono_Id(bonoId);
    }

    public void eliminarCalculo(Long id) {
        calculoRepository.deleteById(id);
    }
}