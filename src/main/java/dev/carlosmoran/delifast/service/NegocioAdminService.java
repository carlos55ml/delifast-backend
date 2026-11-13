package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.GestorNegocio;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.domain.Usuario;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.admin.AltaNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.DatosNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.NegocioAdminResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.GestorNegocioRepository;
import dev.carlosmoran.delifast.repository.LocalidadRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.supabase.NuevaCuenta;
import dev.carlosmoran.delifast.supabase.SupabaseAuthAdmin;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Gestión de negocios por el administrador (RF-28, RF-29). */
@Service
public class NegocioAdminService {

    private final NegocioRepository negocios;
    private final LocalidadRepository localidades;
    private final GestorNegocioRepository gestores;
    private final RepartidorRepository repartidores;
    private final UsuarioRepository usuarios;
    private final PedidoRepository pedidos;
    private final SupabaseAuthAdmin authAdmin;
    private final TransactionTemplate tx;
    private final Clock clock;

    public NegocioAdminService(NegocioRepository negocios, LocalidadRepository localidades,
                               GestorNegocioRepository gestores, RepartidorRepository repartidores,
                               UsuarioRepository usuarios, PedidoRepository pedidos,
                               SupabaseAuthAdmin authAdmin, TransactionTemplate tx, Clock clock) {
        this.negocios = negocios;
        this.localidades = localidades;
        this.gestores = gestores;
        this.repartidores = repartidores;
        this.usuarios = usuarios;
        this.pedidos = pedidos;
        this.authAdmin = authAdmin;
        this.tx = tx;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NegocioAdminResponse> listar(Boolean activo) {
        List<Negocio> lista = activo == null
                ? negocios.findAllByOrderByNombreAsc()
                : negocios.findByActivoOrderByNombreAsc(activo);
        return aRespuestas(lista);
    }

    /**
     * Crea el negocio y la cuenta de su dueño. El negocio se guarda antes de llamar a
     * Supabase porque el trigger que crea el perfil del dueño apunta a él (clave foránea),
     * y Supabase lo ejecuta en su propia transacción. Si la invitación falla, se borra.
     */
    public NegocioAdminResponse alta(AltaNegocioRequest datos) {
        String email = datos.dueno().email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        Long negocioId = tx.execute(estado -> {
            Negocio negocio = new Negocio(localidad(datos.localidadId()), datos.nombre().trim(),
                    datos.telefono().trim(), datos.direccion().trim());
            return negocios.save(negocio).getId();
        });

        UUID duenoId;
        try {
            duenoId = authAdmin.invitar(new NuevaCuenta(email, datos.dueno().nombre().trim(), Rol.NEGOCIO, negocioId, true));
        } catch (RuntimeException e) {
            tx.executeWithoutResult(estado -> negocios.deleteById(negocioId));
            throw e;
        }

        NegocioAdminResponse.Dueno dueno = new NegocioAdminResponse.Dueno(duenoId, datos.dueno().nombre().trim(), email);
        return tx.execute(estado -> aRespuesta(buscar(negocioId), dueno, 0));
    }

    @Transactional
    public NegocioAdminResponse editar(Long id, DatosNegocioRequest datos) {
        Negocio negocio = buscar(id);
        negocio.setNombre(datos.nombre().trim());
        negocio.setLocalidad(localidad(datos.localidadId()));
        negocio.setDireccion(datos.direccion().trim());
        negocio.setTelefono(datos.telefono().trim());
        return aRespuestas(List.of(negocio)).getFirst();
    }

    /**
     * Activa o suspende el negocio. Al suspender se bloquean en Supabase sus cuentas (gestores
     * y repartidores), que no podrán renovar la sesión; al reactivar se desbloquean. El hook
     * del token ya impide iniciar sesión mientras el negocio está suspendido.
     */
    @Transactional
    public NegocioAdminResponse cambiarActivo(Long id, boolean activo) {
        Negocio negocio = buscar(id);
        if (negocio.isActivo() != activo) {
            negocio.setActivo(activo);
            Stream.concat(gestores.findByNegocioId(id).stream(), repartidores.findByNegocioId(id).stream())
                    .map(Usuario::getId)
                    .forEach(cuenta -> authAdmin.bloquear(cuenta, !activo));
        }
        return aRespuestas(List.of(negocio)).getFirst();
    }

    private Negocio buscar(Long id) {
        return negocios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el negocio " + id));
    }

    private Localidad localidad(Long id) {
        return localidades.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La localidad " + id + " no existe"));
    }

    private List<NegocioAdminResponse> aRespuestas(List<Negocio> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Negocio::getId).toList();
        Map<Long, GestorNegocio> duenos = gestores.findDuenos(ids).stream()
                .collect(Collectors.toMap(g -> g.getNegocio().getId(), Function.identity()));
        Map<Long, Long> pedidosHoy = pedidos.contarPorNegocioDesde(Hoy.inicio(clock)).stream()
                .collect(Collectors.toMap(PedidoRepository.PedidosPorNegocio::getNegocioId,
                        PedidoRepository.PedidosPorNegocio::getPedidos));
        return lista.stream().map(n -> {
            GestorNegocio g = duenos.get(n.getId());
            NegocioAdminResponse.Dueno dueno = g == null ? null
                    : new NegocioAdminResponse.Dueno(g.getId(), g.getNombre(), g.getEmail());
            return aRespuesta(n, dueno, pedidosHoy.getOrDefault(n.getId(), 0L));
        }).toList();
    }

    private NegocioAdminResponse aRespuesta(Negocio n, NegocioAdminResponse.Dueno dueno, long pedidosHoy) {
        LocalDateTime ahora = Hoy.ahoraLocal(clock);
        return new NegocioAdminResponse(n.getId(), n.getNombre(), LocalidadResponse.de(n.getLocalidad()),
                n.getDireccion(), n.getTelefono(), dueno, pedidosHoy, n.estaAbierto(ahora), n.isActivo(),
                n.getFechaAlta());
    }
}
