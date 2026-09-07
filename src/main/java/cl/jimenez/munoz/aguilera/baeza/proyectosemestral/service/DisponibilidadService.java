package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.BloqueHorario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.HorarioCancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.HorarioCanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DisponibilidadService {

    private final HorarioCanchaRepository horarioCanchaRepository;
    private final ReservaRepository reservaRepository;
    private final TarifaService tarifaService;

    public DisponibilidadService(HorarioCanchaRepository horarioCanchaRepository,
            ReservaRepository reservaRepository,
            TarifaService tarifaService) {
        this.horarioCanchaRepository = horarioCanchaRepository;
        this.reservaRepository = reservaRepository;
        this.tarifaService = tarifaService;
    }

    public HorarioCancha obtenerOCrearHorario(Cancha cancha, int diaSemana) {
        Optional<HorarioCancha> opt = horarioCanchaRepository.findByCanchaAndDiaSemana(cancha, diaSemana);
        if (opt.isPresent()) {
            return opt.get();
        }
        HorarioCancha def = new HorarioCancha(cancha, diaSemana, LocalTime.of(8, 0), LocalTime.of(23, 0), true);
        return horarioCanchaRepository.save(def);
    }

    public List<HorarioCancha> obtenerHorariosSemanales(Cancha cancha) {
        List<HorarioCancha> lista = horarioCanchaRepository.findByCanchaOrderByDiaSemanaAsc(cancha);
        if (lista.isEmpty()) {
            for (int i = 1; i <= 7; i++) {
                obtenerOCrearHorario(cancha, i);
            }
            lista = horarioCanchaRepository.findByCanchaOrderByDiaSemanaAsc(cancha);
        }
        return lista;
    }

    public List<BloqueHorario> obtenerDisponibilidadDia(Cancha cancha, LocalDate fecha) {
        List<BloqueHorario> bloques = new ArrayList<>();
        int diaSemana = fecha.getDayOfWeek().getValue();

        HorarioCancha horario = obtenerOCrearHorario(cancha, diaSemana);
        if (!horario.isAbierto()) {
            return bloques;
        }

        List<Reserva> reservasDelDia = reservaRepository.findReservasActivasPorCanchaYFecha(cancha, fecha);

        LocalTime cursor = horario.getHoraApertura();
        LocalTime fin = horario.getHoraCierre();

        while (cursor.isBefore(fin)) {
            LocalTime siguiente = cursor.plusHours(1);
            if (siguiente.isAfter(fin)) {
                siguiente = fin;
            }

            boolean ocupada = false;
            for (Reserva r : reservasDelDia) {
                boolean traslape = cursor.isBefore(r.getHoraFin()) && siguiente.isAfter(r.getHoraInicio());
                if (traslape) {
                    ocupada = true;
                    break;
                }
            }

            int precio = tarifaService.calcularPrecioHora(cancha, fecha, cursor);
            boolean peak = tarifaService.esHoraPeak(cancha, fecha, cursor);

            BloqueHorario bloque = new BloqueHorario(
                    cursor,
                    siguiente,
                    !ocupada,
                    ocupada ? "Reservada" : "Disponible",
                    precio,
                    peak);
            bloques.add(bloque);

            cursor = cursor.plusHours(1);
        }

        return bloques;
    }

    public boolean validarDisponibilidad(Cancha cancha, LocalDate fecha, LocalTime horaInicio, int duracionMinutos) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        HorarioCancha horario = obtenerOCrearHorario(cancha, diaSemana);

        if (!horario.isAbierto()) {
            return false;
        }

        LocalTime horaFin = horaInicio.plusMinutes(duracionMinutos);
        if (horaInicio.isBefore(horario.getHoraApertura()) || horaFin.isAfter(horario.getHoraCierre())) {
            return false;
        }

        List<Reserva> reservasDelDia = reservaRepository.findReservasActivasPorCanchaYFecha(cancha, fecha);
        for (Reserva r : reservasDelDia) {
            boolean traslape = horaInicio.isBefore(r.getHoraFin()) && horaFin.isAfter(r.getHoraInicio());
            if (traslape) {
                return false;
            }
        }

        return true;
    }
}
