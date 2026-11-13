package dev.carlosmoran.delifast.supabase;

import dev.carlosmoran.delifast.domain.Rol;

/** Cuenta que crea el backend: el rol y el negocio van en app_metadata, que el usuario no puede cambiar. */
public record NuevaCuenta(String email, String nombre, Rol rol, Long negocioId, boolean esDueno) {
}
