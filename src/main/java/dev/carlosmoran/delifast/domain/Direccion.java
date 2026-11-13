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

/** Dirección guardada por un cliente (RF-04). Solo una puede ser la predeterminada. */
@Entity
@Table(name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "localidad_id", nullable = false)
    private Localidad localidad;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private String calle;

    @Column(name = "codigo_postal", nullable = false)
    private String codigoPostal;

    private String indicaciones;

    private Double latitud;

    private Double longitud;

    @Column(nullable = false)
    private boolean predeterminada;

    protected Direccion() {
    }

    public Direccion(Cliente cliente, Localidad localidad, String alias, String calle, String codigoPostal) {
        this.cliente = cliente;
        this.localidad = localidad;
        this.alias = alias;
        this.calle = calle;
        this.codigoPostal = codigoPostal;
    }

    public Long getId() { return id; }

    public Cliente getCliente() { return cliente; }

    public Localidad getLocalidad() { return localidad; }
    public void setLocalidad(Localidad localidad) { this.localidad = localidad; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public boolean isPredeterminada() { return predeterminada; }
    public void setPredeterminada(boolean predeterminada) { this.predeterminada = predeterminada; }
}
