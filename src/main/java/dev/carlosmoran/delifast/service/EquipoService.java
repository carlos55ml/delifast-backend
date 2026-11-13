package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.GestorNegocio;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.domain.Usuario;
import dev.carlosmoran.delifast.dto.negocio.AltaMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.DatosMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.EmpleadoNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.RepartidorNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.GestorNegocioRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.supabase.NuevaCuenta;
import dev.carlosmoran.delifast.supabase.SupabaseAuthAdmin;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equipo del negocio (N04, RF-19): empleados y repartidores. Cualquiera del negocio los ve, edita
 * y activa o desactiva; solo el dueño da de alta cuentas nuevas. Desactivar bloquea la cuenta en
 * Supabase (no puede renovar la sesión) y el hook del token le impide volver a entrar.
 */
@Service
public class EquipoService {

    private final NegocioActual negocioActual;
    private final GestorNegocioRepository gestores;
    private final RepartidorRepository repartidores;
    private final RepartoRepository repartos;
    private final UsuarioRepository usuarios;
    private final SupabaseAuthAdmin authAdmin;
    private final Clock clock;

    public EquipoService(NegocioActual negocioActual, GestorNegocioRepository gestores,
                         RepartidorRepository repartidores, RepartoRepository repartos, UsuarioRepository usuarios,
                         SupabaseAuthAdmin authAdmin, Clock clock) {
        this.negocioActual = negocioActual;
        this.gestores = gestores;
        this.repartidores = repartidores;
        this.repartos = repartos;
        this.usuarios = usuarios;
        this.authAdmin = authAdmin;
        this.clock = clock;
    }

    // ---- Empleados ----

    /** El dueño primero y luego los empleados por nombre. */
    @Transactional(readOnly = true)
    public List<EmpleadoNegocioResponse> empleados(UsuarioAutenticado usuario) {
        return gestores.findByNegocioId(negocioActual.id(usuario)).stream()
                .sorted(Comparator.comparing((GestorNegocio g) -> !g.isEsDueno())
                        .thenComparing(GestorNegocio::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(EmpleadoNegocioResponse::de)
                .toList();
    }

    public EmpleadoNegocioResponse altaEmpleado(UsuarioAutenticado usuario, AltaMiembroRequest datos) {
        CuentaCreada cuenta = invitar(usuario, datos, Rol.NEGOCIO);
        return new EmpleadoNegocioResponse(cuenta.id, cuenta.datos.nombre(), cuenta.datos.email(),
                cuenta.datos.telefono(), false, true);
    }

    @Transactional
    public EmpleadoNegocioResponse editarEmpleado(UsuarioAutenticado usuario, UUID id, DatosMiembroRequest datos) {
        GestorNegocio empleado = empleado(usuario, id);
        aplicar(empleado, datos);
        return EmpleadoNegocioResponse.de(empleado);
    }

    @Transactional
    public EmpleadoNegocioResponse cambiarActivoEmpleado(UsuarioAutenticado usuario, UUID id, boolean activo) {
        GestorNegocio empleado = empleado(usuario, id);
        if (empleado.isEsDueno() && !activo) {
            throw new ReglaNegocioException("No se puede desactivar al dueño del negocio");
        }
        cambiarActivo(empleado, activo);
        return EmpleadoNegocioResponse.de(empleado);
    }

    // ---- Repartidores ----

    @Transactional(readOnly = true)
    public List<RepartidorNegocioResponse> repartidores(UsuarioAutenticado usuario, Boolean soloDisponibles) {
        Long negocioId = negocioActual.id(usuario);
        List<Repartidor> lista = Boolean.TRUE.equals(soloDisponibles)
                ? repartidores.findByNegocioIdAndActivoTrueAndDisponibleTrueOrderByNombreAsc(negocioId)
                : repartidores.findByNegocioIdOrderByNombreAsc(negocioId);
        Map<UUID, Long> enReparto = porRepartidor(repartos.contarEnRepartoPorRepartidor(negocioId));
        Map<UUID, Long> hoy = porRepartidor(repartos.contarAsignadosDesde(negocioId, Hoy.inicio(clock)));
        return lista.stream().map(r -> respuesta(r, enReparto, hoy)).toList();
    }

    public RepartidorNegocioResponse altaRepartidor(UsuarioAutenticado usuario, AltaMiembroRequest datos) {
        CuentaCreada cuenta = invitar(usuario, datos, Rol.REPARTIDOR);
        return new RepartidorNegocioResponse(cuenta.id, cuenta.datos.nombre(), cuenta.datos.email(),
                cuenta.datos.telefono(), true, false, 0, 0);
    }

    @Transactional
    public RepartidorNegocioResponse editarRepartidor(UsuarioAutenticado usuario, UUID id, DatosMiembroRequest datos) {
        Repartidor repartidor = repartidor(usuario, id);
        aplicar(repartidor, datos);
        return respuesta(repartidor);
    }

    /** Desactivado no aparece en N08; sus repartos EN_REPARTO siguen asignados a él. */
    @Transactional
    public RepartidorNegocioResponse cambiarActivoRepartidor(UsuarioAutenticado usuario, UUID id, boolean activo) {
        Repartidor repartidor = repartidor(usuario, id);
        cambiarActivo(repartidor, activo);
        return respuesta(repartidor);
    }

    // ---- Comunes ----

    private record CuentaCreada(UUID id, AltaMiembroRequest datos) {
    }

    /** Solo el dueño; la cuenta la crea Supabase (y su perfil, el trigger de V1). */
    private CuentaCreada invitar(UsuarioAutenticado usuario, AltaMiembroRequest datos, Rol rol) {
        if (!usuario.esDueno()) {
            throw new AccessDeniedException("Solo el dueño del negocio puede dar de alta cuentas");
        }
        Long negocioId = negocioActual.id(usuario);
        String email = datos.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        AltaMiembroRequest limpio = new AltaMiembroRequest(datos.nombre().trim(), textoOpcional(datos.telefono()), email);
        UUID id = authAdmin.invitar(new NuevaCuenta(
                email, limpio.nombre(), limpio.telefono(), rol, negocioId, false));
        return new CuentaCreada(id, limpio);
    }

    private void cambiarActivo(Usuario cuenta, boolean activo) {
        if (cuenta.isActivo() != activo) {
            cuenta.setActivo(activo);
            authAdmin.bloquear(cuenta.getId(), !activo);
        }
    }

    private static void aplicar(Usuario cuenta, DatosMiembroRequest datos) {
        cuenta.setNombre(datos.nombre().trim());
        cuenta.setTelefono(textoOpcional(datos.telefono()));
    }

    private GestorNegocio empleado(UsuarioAutenticado usuario, UUID id) {
        return gestores.findByIdAndNegocioId(id, negocioActual.id(usuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe esa persona en el negocio"));
    }

    private Repartidor repartidor(UsuarioAutenticado usuario, UUID id) {
        return repartidores.findByIdAndNegocioId(id, negocioActual.id(usuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese repartidor en el negocio"));
    }

    private RepartidorNegocioResponse respuesta(Repartidor r) {
        Long negocioId = r.getNegocio().getId();
        return respuesta(r, porRepartidor(repartos.contarEnRepartoPorRepartidor(negocioId)),
                porRepartidor(repartos.contarAsignadosDesde(negocioId, Hoy.inicio(clock))));
    }

    private static RepartidorNegocioResponse respuesta(Repartidor r, Map<UUID, Long> enReparto, Map<UUID, Long> hoy) {
        return new RepartidorNegocioResponse(r.getId(), r.getNombre(), r.getEmail(), r.getTelefono(), r.isActivo(),
                r.isDisponible(), enReparto.getOrDefault(r.getId(), 0L), hoy.getOrDefault(r.getId(), 0L));
    }

    private static Map<UUID, Long> porRepartidor(List<RepartoRepository.RepartosPorRepartidor> filas) {
        return filas.stream().collect(Collectors.toMap(RepartoRepository.RepartosPorRepartidor::getRepartidorId,
                RepartoRepository.RepartosPorRepartidor::getRepartos));
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
