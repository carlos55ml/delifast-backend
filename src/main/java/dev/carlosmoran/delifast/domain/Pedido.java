package dev.carlosmoran.delifast.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de un cliente a un negocio. Dirección, teléfono, gastos de envío y
 * precios de las líneas son copias: el pedido no cambia si luego cambian.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número legible para el cliente ("DF-000123"); lo asigna la base de datos. */
    @Generated(event = EventType.INSERT)
    @Column(nullable = false, unique = true, insertable = false, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado = EstadoPedido.RECIBIDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    private String nota;

    @Column(name = "direccion_entrega", nullable = false)
    private String direccionEntrega;

    @Column(name = "telefono_contacto", nullable = false)
    private String telefonoContacto;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "gastos_envio", nullable = false, precision = 8, scale = 2)
    private BigDecimal gastosEnvio;

    /** Siempre subtotal + gastosEnvio (lo comprueba también la base de datos). */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal total;

    @Column(name = "tiempo_estimado_min")
    private Integer tiempoEstimadoMin;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaPedido> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha")
    private List<CambioEstado> historial = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", fetch = FetchType.LAZY)
    private Reparto reparto;

    protected Pedido() {
    }

    public Pedido(Cliente cliente, Negocio negocio, MetodoPago metodoPago,
                  String direccionEntrega, String telefonoContacto) {
        this.cliente = cliente;
        this.negocio = negocio;
        this.metodoPago = metodoPago;
        this.direccionEntrega = direccionEntrega;
        this.telefonoContacto = telefonoContacto;
        this.gastosEnvio = negocio.getGastosEnvio();
    }

    public void anadirLinea(Producto producto, int cantidad) {
        lineas.add(new LineaPedido(this, producto, cantidad));
    }

    /** Recalcula subtotal y total con los precios copiados en las líneas (RF-08). */
    public void calcularTotal() {
        subtotal = lineas.stream()
                .map(LineaPedido::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(gastosEnvio);
    }

    /** Cambia el estado y deja constancia en el historial. Las reglas de transición van en el servicio. */
    public void cambiarEstado(EstadoPedido nuevo, Usuario autor) {
        historial.add(new CambioEstado(this, autor, estado, nuevo));
        estado = nuevo;
    }

    public boolean puedeCancelarse() {
        return estado == EstadoPedido.RECIBIDO;
    }

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }

    public Cliente getCliente() { return cliente; }

    public Negocio getNegocio() { return negocio; }

    public EstadoPedido getEstado() { return estado; }

    public MetodoPago getMetodoPago() { return metodoPago; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public String getDireccionEntrega() { return direccionEntrega; }

    public String getTelefonoContacto() { return telefonoContacto; }

    public BigDecimal getSubtotal() { return subtotal; }

    public BigDecimal getGastosEnvio() { return gastosEnvio; }

    public BigDecimal getTotal() { return total; }

    public Integer getTiempoEstimadoMin() { return tiempoEstimadoMin; }
    public void setTiempoEstimadoMin(Integer tiempoEstimadoMin) { this.tiempoEstimadoMin = tiempoEstimadoMin; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }

    public List<LineaPedido> getLineas() { return lineas; }

    public List<CambioEstado> getHistorial() { return historial; }

    public Reparto getReparto() { return reparto; }
}
