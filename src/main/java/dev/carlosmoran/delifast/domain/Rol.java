package dev.carlosmoran.delifast.domain;

/** Rol de la cuenta. Se guarda en perfiles.rol y es el discriminador de Usuario. */
public enum Rol {
    CLIENTE, NEGOCIO, REPARTIDOR, ADMINISTRADOR
}
