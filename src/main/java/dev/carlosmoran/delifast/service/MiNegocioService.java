package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.DiaSemana;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.HorarioApertura;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.negocio.DatosMiNegocioRequest;
import dev.carlosmoran.delifast.dto.negocio.FranjaHorarioDto;
import dev.carlosmoran.delifast.dto.negocio.MiNegocioResponse;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Datos del propio negocio para su panel: cabecera, configuración (N07) y horario. */
@Service
public class MiNegocioService {

    private final NegocioActual negocioActual;
    private final PedidoRepository pedidos;
    private final Imagenes imagenes;
    private final Clock clock;

    public MiNegocioService(NegocioActual negocioActual, PedidoRepository pedidos, Imagenes imagenes, Clock clock) {
        this.imagenes = imagenes;
        this.negocioActual = negocioActual;
        this.pedidos = pedidos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MiNegocioResponse obtener(UsuarioAutenticado usuario) {
        return aRespuesta(negocioActual.cargar(usuario));
    }

    /** Interruptor "Abierto" de la cabecera (RF-18). */
    @Transactional
    public MiNegocioResponse cambiarAbierto(UsuarioAutenticado usuario, boolean abierto) {
        Negocio negocio = negocioActual.cargar(usuario);
        negocio.setAbiertoManual(abierto);
        return aRespuesta(negocio);
    }

    /** Datos de N07 y, si llega, logo nuevo (el anterior se borra al confirmar). */
    @Transactional
    public MiNegocioResponse editar(UsuarioAutenticado usuario, DatosMiNegocioRequest datos, MultipartFile logo) {
        Negocio negocio = negocioActual.cargar(usuario);
        negocio.setNombre(datos.nombre().trim());
        negocio.setDescripcion(textoOpcional(datos.descripcion()));
        negocio.setTelefono(datos.telefono().trim());
        negocio.setDireccion(datos.direccion().trim());
        negocio.setGastosEnvio(datos.gastosEnvio());
        negocio.setPedidoMinimo(datos.pedidoMinimo());
        if (logo != null && !logo.isEmpty()) {
            String anterior = negocio.getLogoUrl();
            negocio.setLogoUrl(imagenes.subir(logo, "logo", Imagenes.BUCKET_LOGOS, negocio.getId()));
            imagenes.borrarAlConfirmar(anterior);
        }
        return aRespuesta(negocio);
    }

    /**
     * Sustituye el horario completo. Ninguna franja puede solaparse con otra, contando las que
     * pasan de medianoche (domingo 23:00-01:00 choca con lunes 00:30-...).
     */
    @Transactional
    public MiNegocioResponse cambiarHorarios(UsuarioAutenticado usuario, List<FranjaHorarioDto> franjas) {
        validarHorarios(franjas);
        Negocio negocio = negocioActual.cargar(usuario);
        negocio.getHorarios().clear();
        franjas.forEach(f -> negocio.getHorarios().add(new HorarioApertura(negocio,
                DiaSemana.deNumero(f.dia().shortValue()), f.horaApertura(), f.horaCierre())));
        return aRespuesta(negocio);
    }

    static void validarHorarios(List<FranjaHorarioDto> franjas) {
        List<String> errores = new ArrayList<>();
        int semana = 7 * 24 * 60;
        for (int i = 0; i < franjas.size(); i++) {
            FranjaHorarioDto f = franjas.get(i);
            if (f.apertura().equals(f.cierre())) {
                errores.add("horarios[" + i + "].cierre: debe ser distinta de la apertura");
            }
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
        // Cada franja como intervalo [inicio, fin) en minutos desde el lunes 00:00.
        int[][] intervalos = new int[franjas.size()][];
        for (int i = 0; i < franjas.size(); i++) {
            FranjaHorarioDto f = franjas.get(i);
            int inicio = (f.dia() - 1) * 1440 + f.horaApertura().toSecondOfDay() / 60;
            int duracion = Math.floorMod(f.horaCierre().toSecondOfDay() / 60 - f.horaApertura().toSecondOfDay() / 60, 1440);
            intervalos[i] = new int[] {inicio, inicio + duracion};
        }
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                if (solapan(intervalos[i], intervalos[j], semana)) {
                    errores.add("horarios[" + j + "]: se solapa con la franja " + (i + 1)
                            + " (" + franjas.get(i).apertura() + "-" + franjas.get(i).cierre() + ")");
                }
            }
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
    }

    /** Solape en una semana circular: el domingo por la noche toca con el lunes. */
    private static boolean solapan(int[] a, int[] b, int semana) {
        for (int desplazamiento : new int[] {-semana, 0, semana}) {
            if (a[0] < b[1] + desplazamiento && b[0] + desplazamiento < a[1]) {
                return true;
            }
        }
        return false;
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private MiNegocioResponse aRespuesta(Negocio n) {
        long pedidosHoy = pedidos.countByNegocioIdAndFechaCreacionGreaterThanEqualAndEstadoNot(
                n.getId(), Hoy.inicio(clock), EstadoPedido.CANCELADO);
        List<FranjaHorarioDto> horarios = n.getHorarios().stream()
                .sorted(Comparator.comparing((HorarioApertura h) -> h.getDia()).thenComparing(HorarioApertura::getHoraApertura))
                .map(FranjaHorarioDto::de)
                .toList();
        return new MiNegocioResponse(n.getId(), n.getNombre(), n.getDescripcion(), LocalidadResponse.de(n.getLocalidad()),
                n.getDireccion(), n.getTelefono(), n.getLogoUrl(), n.getGastosEnvio(), n.getPedidoMinimo(), horarios,
                n.estaAbierto(Hoy.ahoraLocal(clock)), n.isAbiertoManual(), pedidosHoy);
    }
}
