package dev.carlosmoran.delifast.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Cliente de la app móvil. */
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Usuario {

    protected Cliente() {
    }

    @Override
    public Rol getRol() { return Rol.CLIENTE; }
}
