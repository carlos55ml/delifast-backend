package dev.carlosmoran.delifast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Repartidor de un negocio. El negocio solo le asigna pedidos si está activo y disponible. */
@Entity
@DiscriminatorValue("REPARTIDOR")
public class Repartidor extends Usuario {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private boolean disponible;

    protected Repartidor() {
    }

    @Override
    public Rol getRol() { return Rol.REPARTIDOR; }

    public Negocio getNegocio() { return negocio; }

    public boolean isDisponible() { return disponible; }

    public void cambiarDisponibilidad(boolean disponible) { this.disponible = disponible; }
}
