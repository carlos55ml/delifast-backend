package dev.carlosmoran.delifast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cuenta de usuario (tabla perfiles). Una sola tabla para toda la jerarquía,
 * con rol como discriminador. La fila la crea el trigger de alta al registrarse
 * el usuario en Supabase Auth, así que el id es el de auth.users y el backend
 * no inserta perfiles: los lee y los actualiza.
 */
@Entity
@Table(name = "perfiles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rol", discriminatorType = DiscriminatorType.STRING)
public abstract class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    protected Usuario() {
    }

    public abstract Rol getRol();

    public UUID getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public OffsetDateTime getFechaAlta() { return fechaAlta; }
}
