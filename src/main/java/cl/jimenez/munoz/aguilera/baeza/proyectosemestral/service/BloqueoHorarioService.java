package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BloqueoHorarioService {

    private final BloqueoHorarioRepository bloqueoRepository;
    private final CanchaRepository canchaRepository;
    private final ReservaRepository reservaRepository;

    public BloqueoHorarioService(BloqueoHorarioRepository bloqueoRepository,
                                CanchaRepository canchaRepository, ReservaRepository reservaRepository) {
        this.bloqueoRepository = bloqueoRepository;
        this.canchaRepository = canchaRepository;
        this.reservaRepository = reservaRepository;
    }

    public Cancha obtenerCanchaAdministrable(Long canchaId, Usuario usuario) {
        Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new IllegalArgumentException("La cancha no existe."));
        validarPermiso(cancha, usuario);
        return cancha;
    }

    private void validarPermiso(Cancha cancha, Usuario usuario) {
        boolean autorizado = usuario != null && usuario.getId() != null && usuario.isActivo()
                && ("ADMIN_SISTEMA".equals(usuario.getRol())
                || ("ADMIN_COMPLEJO".equals(usuario.getRol()) && cancha.getComplejo().getDueno() != null
                && usuario.getId().equals(cancha.getComplejo().getDueno().getId())));
        if (!autorizado || !cancha.isActivo() || !cancha.getComplejo().isActivo()) {
            throw new IllegalArgumentException("No tienes permisos para gestionar los bloqueos de esta cancha.");
        }
    }

    public List<BloqueoHorario> listar(Cancha cancha) {
        return bloqueoRepository.findByCanchaOrderByFechaInicioAscHoraInicioAsc(cancha);
    }

    @Transactional
    public void crear(Long canchaId, Usuario usuario, LocalDate fechaInicio, LocalDate fechaFin,
                      boolean diaCompleto, LocalTime horaInicio, LocalTime horaFin, String motivo) {
        Cancha cancha = canchaRepository.findByIdParaActualizar(canchaId)
                .orElseThrow(() -> new IllegalArgumentException("La cancha no existe."));
        validarPermiso(cancha, usuario);

        if (fechaInicio == null || fechaFin == null || fechaInicio.isBefore(LocalDate.now())
                || fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("Indica un rango de fechas válido, desde hoy en adelante.");
        }
        if (!diaCompleto && (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin))) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin, dentro del mismo día.");
        }
        if (motivo == null || motivo.isBlank() || motivo.strip().length() > 250) {
            throw new IllegalArgumentException("El motivo es obligatorio y debe tener como máximo 250 caracteres.");
        }

        BloqueoHorario nuevo = new BloqueoHorario();
        nuevo.setCancha(cancha);
        nuevo.setFechaInicio(fechaInicio);
        nuevo.setFechaFin(fechaFin);
        nuevo.setDiaCompleto(diaCompleto);
        nuevo.setHoraInicio(diaCompleto ? null : horaInicio);
        nuevo.setHoraFin(diaCompleto ? null : horaFin);
        nuevo.setMotivo(motivo.strip());

        for (BloqueoHorario existente : bloqueoRepository.findPorRango(cancha, fechaInicio, fechaFin)) {
            if (diaCompleto || existente.isDiaCompleto()
                    || (horaInicio.isBefore(existente.getHoraFin()) && horaFin.isAfter(existente.getHoraInicio()))) {
                throw new IllegalArgumentException("Ya existe un bloqueo que se cruza con esas fechas y horas.");
            }
        }
        for (Reserva reserva : reservaRepository.findReservasActivasPorCanchaYRango(cancha, fechaInicio, fechaFin)) {
            if (nuevo.seSuperpone(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin())) {
                throw new IllegalArgumentException("Existe una reserva vigente el " + reserva.getFecha()
                        + " a las " + reserva.getHoraInicio() + ". Resuélvela antes de bloquear ese horario.");
            }
        }
        bloqueoRepository.save(nuevo);
    }

    @Transactional
    public void eliminar(Long canchaId, Long bloqueoId, Usuario usuario) {
        Cancha cancha = canchaRepository.findByIdParaActualizar(canchaId)
                .orElseThrow(() -> new IllegalArgumentException("La cancha no existe."));
        validarPermiso(cancha, usuario);
        BloqueoHorario bloqueo = bloqueoRepository.findById(bloqueoId)
                .orElseThrow(() -> new IllegalArgumentException("El bloqueo no existe."));
        if (!canchaId.equals(bloqueo.getCancha().getId_cancha())) {
            throw new IllegalArgumentException("El bloqueo no pertenece a esta cancha.");
        }
        bloqueoRepository.delete(bloqueo);
    }
}
