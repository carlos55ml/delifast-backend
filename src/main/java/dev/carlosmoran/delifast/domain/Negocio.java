package dev.carlosmoran.delifast.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "negocios")
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "localidad_id", nullable = false)
    private Localidad localidad;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String direccion;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "gastos_envio", nullable = false, precision = 8, scale = 2)
    private BigDecimal gastosEnvio = BigDecimal.ZERO;

    @Column(name = "pedido_minimo", nullable = false, precision = 8, scale = 2)
    private BigDecimal pedidoMinimo = BigDecimal.ZERO;

    /** Interruptor abierto/cerrado del negocio (RF-18). */
    @Column(name = "abierto_manual", nullable = false)
    private boolean abiertoManual;

    @Column(nullable = false)
    private boolean activo = true;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_alta", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    @OneToMany(mappedBy = "negocio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioApertura> horarios = new ArrayList<>();

    @OneToMany(mappedBy = "negocio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<Categoria> categorias = new ArrayList<>();

    protected Negocio() {
    }

    public Negocio(Localidad localidad, String nombre, String telefono, String direccion) {
        this.localidad = localidad;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    /**
     * Si acepta pedidos en este momento (hora local del negocio): no suspendido, con el
     * interruptor abierto y dentro de alguna franja de horario. Sin horarios configurados
     * manda solo el interruptor.
     */
    public boolean estaAbierto(LocalDateTime ahora) {
        if (!activo || !abiertoManual) {
            return false;
        }
        return horarios.isEmpty() || horarios.stream().anyMatch(h -> h.incluye(ahora));
    }

    public Long getId() { return id; }

    public Localidad getLocalidad() { return localidad; }
    public void setLocalidad(Localidad localidad) { this.localidad = localidad; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public BigDecimal getGastosEnvio() { return gastosEnvio; }
    public void setGastosEnvio(BigDecimal gastosEnvio) { this.gastosEnvio = gastosEnvio; }

    public BigDecimal getPedidoMinimo() { return pedidoMinimo; }
    public void setPedidoMinimo(BigDecimal pedidoMinimo) { this.pedidoMinimo = pedidoMinimo; }

    public boolean isAbiertoManual() { return abiertoManual; }
    public void setAbiertoManual(boolean abiertoManual) { this.abiertoManual = abiertoManual; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public OffsetDateTime getFechaAlta() { return fechaAlta; }

    public List<HorarioApertura> getHorarios() { return horarios; }

    public List<Categoria> getCategorias() { return categorias; }
}
