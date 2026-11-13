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
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Incidencia de un pedido comunicada por el cliente o el repartidor (RF-12, RF-21, RF-26). */
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    /** Quien la responde o resuelve; nulo hasta entonces. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenIncidencia origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIncidencia tipo;

    @Column(nullable = false)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIncidencia estado = EstadoIncidencia.ABIERTA;

    private String respuesta;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    @Column(name = "fecha_resolucion")
    private OffsetDateTime fechaResolucion;

    protected Incidencia() {
    }

    public Incidencia(Pedido pedido, Usuario autor, OrigenIncidencia origen, TipoIncidencia tipo, String descripcion) {
        this.pedido = pedido;
        this.autor = autor;
        this.origen = origen;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public void responder(Usuario responsable, String respuesta) {
        this.responsable = responsable;
        this.respuesta = respuesta;
        this.estado = EstadoIncidencia.RESPONDIDA;
    }

    public void resolver(Usuario responsable) {
        this.responsable = responsable;
        this.estado = EstadoIncidencia.RESUELTA;
        this.fechaResolucion = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public Pedido getPedido() { return pedido; }

    public Usuario getAutor() { return autor; }

    public Usuario getResponsable() { return responsable; }

    public OrigenIncidencia getOrigen() { return origen; }

    public TipoIncidencia getTipo() { return tipo; }

    public String getDescripcion() { return descripcion; }

    public EstadoIncidencia getEstado() { return estado; }

    public String getRespuesta() { return respuesta; }

    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }

    public OffsetDateTime getFechaResolucion() { return fechaResolucion; }
}
