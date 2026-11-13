package dev.carlosmoran.delifast.supabase;

import java.util.UUID;

/**
 * Operaciones de administración de cuentas en Supabase Auth. Es una interfaz para poder
 * sustituirla en los tests sin llamar a Supabase.
 */
public interface SupabaseAuthAdmin {

    /**
     * Crea la cuenta y envía el email de invitación para que el usuario elija su contraseña.
     *
     * @return id de la cuenta (el de auth.users y perfiles)
     * @throws dev.carlosmoran.delifast.exception.ConflictoException si el email ya está registrado
     * @throws ServicioExternoException si Supabase no responde o devuelve otro error
     */
    UUID invitar(NuevaCuenta cuenta);

    /** Borra la cuenta (se usa para deshacer un alta que no ha podido completarse). */
    void eliminar(UUID id);

    /**
     * Bloquea o desbloquea la cuenta. Bloqueada no puede iniciar sesión ni renovar el token,
     * así que sus sesiones abiertas caducan con el access token en curso.
     */
    void bloquear(UUID id, boolean bloqueada);
}
