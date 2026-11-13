package dev.carlosmoran.delifast.controller;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.dto.admin.IncidenciaAdminResponse;
import dev.carlosmoran.delifast.dto.admin.ResumenAdminResponse;
import dev.carlosmoran.delifast.service.AdminConsultaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Incidencias (A04) y resumen (A01) de la zona de administración. */
@RestController
@RequestMapping("/admin")
public class AdminConsultaController {

    private final AdminConsultaService servicio;

    public AdminConsultaController(AdminConsultaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/incidencias")
    public List<IncidenciaAdminResponse> incidencias(
            @RequestParam(required = false) List<EstadoIncidencia> estado,
            @RequestParam(required = false) Long negocioId) {
        return servicio.incidencias(estado, negocioId);
    }

    @GetMapping("/resumen")
    public ResumenAdminResponse resumen() {
        return servicio.resumen();
    }
}
