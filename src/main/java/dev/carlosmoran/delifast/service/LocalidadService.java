package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.repository.LocalidadRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalidadService {

    private final LocalidadRepository localidades;

    public LocalidadService(LocalidadRepository localidades) {
        this.localidades = localidades;
    }

    @Transactional(readOnly = true)
    public List<LocalidadResponse> listar() {
        return localidades.findAllByOrderByNombreAsc().stream().map(LocalidadResponse::de).toList();
    }
}
