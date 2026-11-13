package dev.carlosmoran.delifast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Dueño o empleado de un negocio (rol NEGOCIO). Como mucho un dueño por negocio. */
@Entity
@DiscriminatorValue("NEGOCIO")
public class GestorNegocio extends Usuario {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(name = "es_dueno", nullable = false)
    private boolean esDueno;

    protected GestorNegocio() {
    }

    @Override
    public Rol getRol() { return Rol.NEGOCIO; }

    public Negocio getNegocio() { return negocio; }

    public boolean isEsDueno() { return esDueno; }
}
