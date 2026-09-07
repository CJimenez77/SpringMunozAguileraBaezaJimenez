package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ServicioAdicional;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.TarifaFranja;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.TarifaFranjaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class TarifaService {

    private final TarifaFranjaRepository tarifaFranjaRepository;

    public TarifaService(TarifaFranjaRepository tarifaFranjaRepository) {
        this.tarifaFranjaRepository = tarifaFranjaRepository;
    }

    public Integer calcularPrecioHora(Cancha cancha, LocalDate fecha, LocalTime hora) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        List<TarifaFranja> franjas = tarifaFranjaRepository.findByCanchaAndActivoTrue(cancha);

        for (TarifaFranja f : franjas) {
            boolean diaCoincide = (f.getDiaSemana() == null || f.getDiaSemana().equals(diaSemana));
            if (diaCoincide) {
                boolean dentroHorario = !hora.isBefore(f.getHoraInicio()) && hora.isBefore(f.getHoraFin());
                if (dentroHorario) {
                    return f.getPrecioHora();
                }
            }
        }
        return cancha.getPrecio_hora();
    }

    public boolean esHoraPeak(Cancha cancha, LocalDate fecha, LocalTime hora) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        List<TarifaFranja> franjas = tarifaFranjaRepository.findByCanchaAndActivoTrue(cancha);

        for (TarifaFranja f : franjas) {
            boolean diaCoincide = (f.getDiaSemana() == null || f.getDiaSemana().equals(diaSemana));
            if (diaCoincide) {
                boolean dentroHorario = !hora.isBefore(f.getHoraInicio()) && hora.isBefore(f.getHoraFin());
                if (dentroHorario && f.getPrecioHora() > cancha.getPrecio_hora()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Integer calcularPrecioCanchaTotal(Cancha cancha, LocalDate fecha, LocalTime horaInicio,
            int duracionMinutos) {
        double horas = duracionMinutos / 60.0;
        int precioHora = calcularPrecioHora(cancha, fecha, horaInicio);
        return (int) Math.round(precioHora * horas);
    }

    public Integer calcularPrecioServicios(List<ServicioAdicional> servicios) {
        if (servicios == null || servicios.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ServicioAdicional s : servicios) {
            if (s != null && s.getPrecio() != null) {
                total += s.getPrecio();
            }
        }
        return total;
    }
}
