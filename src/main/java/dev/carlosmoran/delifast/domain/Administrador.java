package dev.carlosmoran.delifast.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Administrador de la plataforma. */
@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {

    protected Administrador() {
    }

    @Override
    public Rol getRol() { return Rol.ADMINISTRADOR; }
}
