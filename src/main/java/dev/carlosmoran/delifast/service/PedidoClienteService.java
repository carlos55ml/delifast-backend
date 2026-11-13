package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.Direccion;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Producto;
import dev.carlosmoran.delifast.dto.cliente.NuevoPedidoRequest;
import dev.carlosmoran.delifast.dto.cliente.PedidoClienteResponse;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.DireccionRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.ProductoRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedidos del cliente (RF-08..RF-10). Al crear, los precios y los gastos de envío salen de la
 * base de datos (nunca de la app) y se copian en el pedido junto con la dirección y el teléfono.
 */
@Service
public class PedidoClienteService {

    private static final int MAXIMO_UNIDADES = 50;

    private final PedidoRepository pedidos;
    private final NegocioRepository negocios;
    private final ProductoRepository productos;
    private final DireccionRepository direcciones;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public PedidoClienteService(PedidoRepository pedidos, NegocioRepository negocios, ProductoRepository productos,
                                DireccionRepository direcciones, UsuarioRepository usuarios, Clock clock) {
        this.pedidos = pedidos;
        this.negocios = negocios;
        this.productos = productos;
        this.direcciones = direcciones;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PedidoClienteResponse> listar(UsuarioAutenticado usuario) {
        return pedidos.findByClienteIdOrderByFechaCreacionDesc(usuario.id()).stream()
                .map(PedidoClienteResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoClienteResponse obtener(UsuarioAutenticado usuario, Long id) {
        return pedidos.findByIdAndClienteId(id, usuario.id())
                .map(PedidoClienteResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + id));
    }

    @Transactional
    public PedidoClienteResponse crear(UsuarioAutenticado usuario, NuevoPedidoRequest datos) {
        Cliente cliente = usuarios.findById(usuario.id())
                .filter(Cliente.class::isInstance).map(Cliente.class::cast)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no es de cliente"));
        Negocio negocio = negocios.findByIdAndActivoTrue(datos.negocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el negocio " + datos.negocioId()));
        if (!negocio.estaAbierto(Hoy.ahoraLocal(clock))) {
            throw new ReglaNegocioException(negocio.getNombre() + " está cerrado ahora mismo");
        }
        Direccion direccion = direcciones.findByIdAndClienteId(datos.direccionId(), usuario.id())
                .orElseThrow(() -> new ReglaNegocioException("La dirección de entrega no es tuya o ya no existe"));
        if (!direccion.getLocalidad().getId().equals(negocio.getLocalidad().getId())) {
            throw new ReglaNegocioException(negocio.getNombre() + " no reparte en " + direccion.getLocalidad().getNombre());
        }
        if (cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            throw new ReglaNegocioException("Añade un teléfono a tu perfil para que el repartidor pueda llamarte");
        }

        Pedido pedido = new Pedido(cliente, negocio, datos.metodoPago(), textoEntrega(direccion), cliente.getTelefono());
        pedido.setPosicionEntrega(direccion.getLatitud(), direccion.getLongitud());
        pedido.setNota(datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim());
        Map<Long, Integer> unidades = unidadesPorProducto(datos);
        Map<Long, Producto> encontrados = productos.findAllById(unidades.keySet()).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        unidades.forEach((productoId, cantidad) -> {
            Producto producto = encontrados.get(productoId);
            if (producto == null || !producto.isActivo()
                    || !producto.getCategoria().getNegocio().getId().equals(negocio.getId())) {
                throw new ReglaNegocioException("El producto " + productoId + " no está en la carta de " + negocio.getNombre());
            }
            if (!producto.isDisponible()) {
                throw new ReglaNegocioException(producto.getNombre() + " está agotado");
            }
            pedido.anadirLinea(producto, cantidad);
        });
        pedido.calcularTotal();
        if (pedido.getSubtotal().compareTo(negocio.getPedidoMinimo()) < 0) {
            throw new ReglaNegocioException("El pedido mínimo de " + negocio.getNombre() + " es de "
                    + negocio.getPedidoMinimo().toPlainString() + " €");
        }
        pedido.registrarCreacion(cliente);
        // saveAndFlush para que la base de datos asigne el código y la fecha (@Generated).
        return PedidoClienteResponse.de(pedidos.saveAndFlush(pedido));
    }

    /** Suma las líneas repetidas del mismo producto, conservando el orden. */
    private static Map<Long, Integer> unidadesPorProducto(NuevoPedidoRequest datos) {
        Map<Long, Integer> unidades = new LinkedHashMap<>();
        datos.lineas().forEach(l -> unidades.merge(l.productoId(), l.cantidad(), Integer::sum));
        unidades.forEach((id, n) -> {
            if (n > MAXIMO_UNIDADES) {
                throw new ReglaNegocioException("No se pueden pedir más de " + MAXIMO_UNIDADES + " unidades de un producto");
            }
        });
        return unidades;
    }

    /** Igual que la app: "calle, CP Localidad" y las indicaciones si las hay. */
    static String textoEntrega(Direccion d) {
        String texto = d.getCalle() + ", " + d.getCodigoPostal() + " " + d.getLocalidad().getNombre();
        return d.getIndicaciones() == null ? texto : texto + " (" + d.getIndicaciones() + ")";
    }
}
