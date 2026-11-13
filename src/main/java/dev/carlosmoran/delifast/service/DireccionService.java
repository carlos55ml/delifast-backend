package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.Direccion;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.dto.cliente.DatosDireccionRequest;
import dev.carlosmoran.delifast.dto.cliente.DireccionResponse;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.repository.DireccionRepository;
import dev.carlosmoran.delifast.repository.LocalidadRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direcciones del cliente (RF-04). Como mucho una predeterminada (índice único parcial en V1):
 * antes de marcar una se desmarca la anterior y se vuelca a la base de datos.
 */
@Service
public class DireccionService {

    private final DireccionRepository direcciones;
    private final LocalidadRepository localidades;
    private final UsuarioRepository usuarios;

    public DireccionService(DireccionRepository direcciones, LocalidadRepository localidades,
                            UsuarioRepository usuarios) {
        this.direcciones = direcciones;
        this.localidades = localidades;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> listar(UsuarioAutenticado usuario) {
        return direcciones.findByClienteIdOrderByPredeterminadaDescAliasAsc(usuario.id()).stream()
                .map(DireccionResponse::de)
                .toList();
    }

    /** La primera dirección queda como predeterminada aunque no se pida. */
    @Transactional
    public DireccionResponse crear(UsuarioAutenticado usuario, DatosDireccionRequest datos) {
        Cliente cliente = usuarios.findById(usuario.id())
                .filter(Cliente.class::isInstance).map(Cliente.class::cast)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no es de cliente"));
        boolean primera = !direcciones.existsByClienteId(usuario.id());
        Direccion direccion = new Direccion(cliente, localidad(datos.localidadId()), datos.alias().trim(),
                datos.calle().trim(), datos.codigoPostal());
        direccion.setIndicaciones(textoOpcional(datos.indicaciones()));
        if (primera || Boolean.TRUE.equals(datos.predeterminada())) {
            desmarcarPredeterminada(usuario, null);
            direccion.setPredeterminada(true);
        }
        return DireccionResponse.de(direcciones.save(direccion));
    }

    @Transactional
    public DireccionResponse editar(UsuarioAutenticado usuario, Long id, DatosDireccionRequest datos) {
        Direccion direccion = buscar(usuario, id);
        direccion.setAlias(datos.alias().trim());
        direccion.setCalle(datos.calle().trim());
        direccion.setCodigoPostal(datos.codigoPostal());
        direccion.setLocalidad(localidad(datos.localidadId()));
        direccion.setIndicaciones(textoOpcional(datos.indicaciones()));
        if (Boolean.TRUE.equals(datos.predeterminada()) && !direccion.isPredeterminada()) {
            desmarcarPredeterminada(usuario, id);
            direccion.setPredeterminada(true);
        }
        return DireccionResponse.de(direccion);
    }

    /** Si era la predeterminada, pasa a serlo la primera que quede. */
    @Transactional
    public void borrar(UsuarioAutenticado usuario, Long id) {
        Direccion direccion = buscar(usuario, id);
        boolean eraPredeterminada = direccion.isPredeterminada();
        direcciones.delete(direccion);
        direcciones.flush();
        if (eraPredeterminada) {
            direcciones.findByClienteIdOrderByPredeterminadaDescAliasAsc(usuario.id()).stream()
                    .findFirst()
                    .ifPresent(d -> d.setPredeterminada(true));
        }
    }

    private void desmarcarPredeterminada(UsuarioAutenticado usuario, Long excepto) {
        direcciones.findByClienteIdOrderByPredeterminadaDescAliasAsc(usuario.id()).stream()
                .filter(d -> d.isPredeterminada() && !d.getId().equals(excepto))
                .forEach(d -> d.setPredeterminada(false));
        direcciones.flush();
    }

    private Direccion buscar(UsuarioAutenticado usuario, Long id) {
        return direcciones.findByIdAndClienteId(id, usuario.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la dirección " + id));
    }

    private Localidad localidad(Long id) {
        return localidades.findById(id)
                .orElseThrow(() -> new ValidacionException("localidadId", "no existe"));
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
