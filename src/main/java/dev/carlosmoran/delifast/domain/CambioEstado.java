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

/** Registro del historial de estados de un pedido (RF-09). */
@Entity
@Table(name = "cambios_estado")
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /** Nulo si la cuenta del autor se borra después. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    /** Nulo en el primer registro, al crear el pedido. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoPedido estadoNuevo;

    @Column(nullable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

    protected CambioEstado() {
    }

    CambioEstado(Pedido pedido, Usuario autor, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo) {
        this.pedido = pedido;
        this.autor = autor;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
    }

    public Long getId() { return id; }

    public Pedido getPedido() { return pedido; }

    public Usuario getAutor() { return autor; }

    public EstadoPedido getEstadoAnterior() { return estadoAnterior; }

    public EstadoPedido getEstadoNuevo() { return estadoNuevo; }

    public OffsetDateTime getFecha() { return fecha; }
}
