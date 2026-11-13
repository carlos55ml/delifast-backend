package dev.carlosmoran.delifast.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entrega de un pedido por un repartidor. Lo crea el negocio al asignar el pedido
 * LISTO a uno de sus repartidores. pedido_id es único en la base de datos: un
 * pedido tiene como mucho un reparto aunque dos personas lo asignen a la vez.
 * La última posición del repartidor (UbicacionRepartidor, RF-27) son columnas
 * de esta tabla.
 */
@Entity
@Table(name = "repartos")
public class Reparto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repartidor_id", nullable = false)
    private Repartidor repartidor;

    @Column(name = "fecha_asignacion", nullable = false)
    private OffsetDateTime fechaAsignacion = OffsetDateTime.now();

    @Column(name = "fecha_entrega")
    private OffsetDateTime fechaEntrega;

    @Column(name = "cobro_confirmado", nullable = false)
    private boolean cobroConfirmado;

    /** Cómo pagó realmente el cliente (RF-25). */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_cobro")
    private MetodoPago metodoCobro;

    private Double latitud;

    private Double longitud;

    @Column(name = "ubicacion_actualizada_en")
    private OffsetDateTime ubicacionActualizadaEn;

    protected Reparto() {
    }

    public Reparto(Pedido pedido, Repartidor repartidor) {
        this.pedido = pedido;
        this.repartidor = repartidor;
    }

    /** Guarda la hora de entrega y el cobro. El pedido pasa a ENTREGADO en el servicio. */
    public void confirmarEntrega(MetodoPago metodo) {
        this.fechaEntrega = OffsetDateTime.now();
        this.metodoCobro = metodo;
        this.cobroConfirmado = true;
    }

    public void actualizarUbicacion(double latitud, double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.ubicacionActualizadaEn = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public Pedido getPedido() { return pedido; }

    public Repartidor getRepartidor() { return repartidor; }

    public OffsetDateTime getFechaAsignacion() { return fechaAsignacion; }

    public OffsetDateTime getFechaEntrega() { return fechaEntrega; }

    public boolean isCobroConfirmado() { return cobroConfirmado; }

    public MetodoPago getMetodoCobro() { return metodoCobro; }

    public Double getLatitud() { return latitud; }

    public Double getLongitud() { return longitud; }

    public OffsetDateTime getUbicacionActualizadaEn() { return ubicacionActualizadaEn; }
}
