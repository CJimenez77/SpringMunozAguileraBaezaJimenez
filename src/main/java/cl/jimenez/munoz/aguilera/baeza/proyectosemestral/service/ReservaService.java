package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final DisponibilidadService disponibilidadService;
    private final TarifaService tarifaService;
    private final WebpaySandboxService webpaySandboxService;
    private final EmailService emailService;

    public ReservaService(ReservaRepository reservaRepository,
            DisponibilidadService disponibilidadService,
            TarifaService tarifaService,
            WebpaySandboxService webpaySandboxService,
            EmailService emailService) {
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.tarifaService = tarifaService;
        this.webpaySandboxService = webpaySandboxService;
        this.emailService = emailService;
    }

    @Transactional
    public Reserva crearReserva(Usuario usuario, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
            int duracionMinutos, List<ServicioAdicional> servicios, String grupoRecurrente) {

        if (!disponibilidadService.validarDisponibilidad(cancha, fecha, horaInicio, duracionMinutos)) {
            throw new IllegalArgumentException("El horario seleccionado no se encuentra disponible.");
        }

        LocalTime horaFin = horaInicio.plusMinutes(duracionMinutos);
        int precioCancha = tarifaService.calcularPrecioCanchaTotal(cancha, fecha, horaInicio, duracionMinutos);
        int precioServicios = tarifaService.calcularPrecioServicios(servicios);
        int precioTotal = precioCancha + precioServicios;

        String codigo = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setCancha(cancha);
        reserva.setFecha(fecha);
        reserva.setHoraInicio(horaInicio);
        reserva.setHoraFin(horaFin);
        reserva.setDuracionMinutos(duracionMinutos);
        reserva.setPrecioCancha(precioCancha);
        reserva.setPrecioServicios(precioServicios);
        reserva.setPrecioTotal(precioTotal);
        reserva.setEstado("PENDIENTE");
        reserva.setCodigoReserva(codigo);
        reserva.setGrupoRecurrente(grupoRecurrente);
        if (servicios != null && !servicios.isEmpty()) {
            reserva.setServicios(new ArrayList<>(servicios));
        }

        Reserva guardada = reservaRepository.save(reserva);
        webpaySandboxService.iniciarTransaccion(guardada, "WEBPAY_PLUS");
        return guardada;
    }

    @Transactional
    public List<Reserva> crearReservasRecurrentes(Usuario usuario, Cancha cancha, LocalDate fechaInicio,
            LocalTime horaInicio, int duracionMinutos,
            List<ServicioAdicional> servicios, int semanasRecurrencia) {

        for (int i = 0; i < semanasRecurrencia; i++) {
            LocalDate fechaObjetivo = fechaInicio.plusWeeks(i);
            if (!disponibilidadService.validarDisponibilidad(cancha, fechaObjetivo, horaInicio, duracionMinutos)) {
                throw new IllegalArgumentException("Conflicto de disponibilidad en la fecha: " + fechaObjetivo);
            }
        }

        String grupoRecurrente = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<Reserva> creadas = new ArrayList<>();

        for (int i = 0; i < semanasRecurrencia; i++) {
            LocalDate fechaObjetivo = fechaInicio.plusWeeks(i);
            Reserva r = crearReserva(usuario, cancha, fechaObjetivo, horaInicio, duracionMinutos, servicios,
                    grupoRecurrente);
            creadas.add(r);
        }

        return creadas;
    }

    @Transactional
    public boolean cancelarReserva(Long reservaId, Usuario solicitante) {
        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return false;
        }

        boolean esPropietario = reserva.getUsuario().getId().equals(solicitante.getId());
        boolean esAdmin = "ADMIN_SISTEMA".equals(solicitante.getRol()) ||
                ("ADMIN_COMPLEJO".equals(solicitante.getRol()) &&
                        reserva.getCancha().getComplejo().getDueno() != null &&
                        reserva.getCancha().getComplejo().getDueno().getId().equals(solicitante.getId()));

        if (!esPropietario && !esAdmin) {
            return false;
        }

        if (!esAdmin && !reserva.esCancelable()) {
            throw new IllegalStateException(
                    "Las cancelaciones solo se permiten con al menos 24 horas de anticipación.");
        }

        reserva.setEstado("CANCELADA");
        reservaRepository.save(reserva);

        webpaySandboxService.reembolsarPago(reserva);
        emailService.enviarCancelacionReserva(reserva);

        return true;
    }
}
