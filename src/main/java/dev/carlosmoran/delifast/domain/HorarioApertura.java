package dev.carlosmoran.delifast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;

/**
 * Franja de apertura de un día. Si horaCierre es anterior a horaApertura, la
 * franja pasa de medianoche (por ejemplo 20:00-00:30).
 */
@Entity
@Table(name = "horarios_apertura")
public class HorarioApertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Convert(converter = DiaSemanaConverter.class)
    @Column(nullable = false)
    private DiaSemana dia;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    protected HorarioApertura() {
    }

    public HorarioApertura(Negocio negocio, DiaSemana dia, LocalTime horaApertura, LocalTime horaCierre) {
        this.negocio = negocio;
        this.dia = dia;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }

    public boolean pasaDeMedianoche() {
        return horaCierre.isBefore(horaApertura);
    }

    public Long getId() { return id; }

    public Negocio getNegocio() { return negocio; }

    public DiaSemana getDia() { return dia; }

    public LocalTime getHoraApertura() { return horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
}
