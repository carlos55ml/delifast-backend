package dev.carlosmoran.delifast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal precio;

    @Column(name = "foto_url")
    private String fotoUrl;

    /** false = agotado: se ve en la carta pero no se puede añadir (RF-06). */
    @Column(nullable = false)
    private boolean disponible = true;

    /** Borrado lógico: un producto que ya está en pedidos no se borra. */
    @Column(nullable = false)
    private boolean activo = true;

    protected Producto() {
    }

    public Producto(Categoria categoria, String nombre, BigDecimal precio) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.precio = precio;
    }

    public void marcarAgotado() { this.disponible = false; }

    public void marcarDisponible() { this.disponible = true; }

    public Long getId() { return id; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public boolean isDisponible() { return disponible; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
