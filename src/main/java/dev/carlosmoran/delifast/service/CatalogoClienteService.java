package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.dto.cliente.CartaClienteResponse;
import dev.carlosmoran.delifast.dto.cliente.NegocioClienteResponse;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.repository.CategoriaRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Negocios y carta que ve el cliente (RF-05, RF-06). Solo negocios activos. */
@Service
@Transactional(readOnly = true)
public class CatalogoClienteService {

    private final NegocioRepository negocios;
    private final CategoriaRepository categorias;
    private final Clock clock;

    public CatalogoClienteService(NegocioRepository negocios, CategoriaRepository categorias, Clock clock) {
        this.negocios = negocios;
        this.categorias = categorias;
        this.clock = clock;
    }

    /** Los abiertos primero y luego por nombre. */
    public List<NegocioClienteResponse> negocios(Long localidadId) {
        List<Negocio> lista = localidadId == null
                ? negocios.findByActivoOrderByNombreAsc(true)
                : negocios.findByActivoTrueAndLocalidadIdOrderByNombreAsc(localidadId);
        LocalDateTime ahora = Hoy.ahoraLocal(clock);
        return lista.stream()
                .map(n -> NegocioClienteResponse.de(n, ahora))
                .sorted(Comparator.comparing((NegocioClienteResponse n) -> !n.abierto()))
                .toList();
    }

    public CartaClienteResponse carta(Long negocioId) {
        Negocio negocio = negocios.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el negocio " + negocioId));
        return new CartaClienteResponse(NegocioClienteResponse.de(negocio, Hoy.ahoraLocal(clock)),
                categorias.findByNegocioIdOrderByOrdenAscIdAsc(negocioId).stream()
                        .map(CartaClienteResponse::categoria)
                        .filter(Objects::nonNull)
                        .toList());
    }
}
