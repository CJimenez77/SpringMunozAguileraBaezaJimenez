package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.TransferenciaReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final DisponibilidadService disponibilidadService;
    private final TarifaService tarifaService;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransferenciaReservaRepository transferenciaReservaRepository;
    private final EmailService emailService;
    private final CanchaRepository canchaRepository;
    private final FidelizacionService fidelizacionService;

    public ReservaService(ReservaRepository reservaRepository,
                          DisponibilidadService disponibilidadService,
                          TarifaService tarifaService,
                          PagoRepository pagoRepository,
                          UsuarioRepository usuarioRepository,
                          TransferenciaReservaRepository transferenciaReservaRepository,
                          EmailService emailService, CanchaRepository canchaRepository,
                          FidelizacionService fidelizacionService) {
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.tarifaService = tarifaService;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.transferenciaReservaRepository = transferenciaReservaRepository;
        this.emailService = emailService;
        this.canchaRepository = canchaRepository;
        this.fidelizacionService = fidelizacionService;
    }

    @Transactional
    public Reserva crearReserva(Usuario usuario, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
                                int duracionMinutos, List<ServicioAdicional> servicios, String grupoRecurrente) {
        boolean aplicarDescuento = fidelizacionService.tieneDescuentoDisponible(usuario);
        return crearReservaConOpciones(usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, grupoRecurrente, aplicarDescuento);
    }

    @Transactional
    public Reserva crearReservaConOpciones(Usuario usuario, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
                                          int duracionMinutos, List<ServicioAdicional> servicios, String grupoRecurrente,
                                          boolean aplicarDescuento) {

        cancha = canchaRepository.findByIdParaActualizar(cancha.getId_cancha())
                .orElseThrow(() -> new IllegalArgumentException("La cancha no existe."));
        if (!disponibilidadService.validarDisponibilidad(cancha, fecha, horaInicio, duracionMinutos)) {
            throw new IllegalArgumentException("El horario seleccionado no se encuentra disponible.");
        }

        LocalTime horaFin = horaInicio.plusMinutes(duracionMinutos);
        int precioCancha = tarifaService.calcularPrecioCanchaTotal(cancha, fecha, horaInicio, duracionMinutos);
        int precioServicios = tarifaService.calcularPrecioServicios(servicios);
        int subtotal = precioCancha + precioServicios;
        int descuento = 0;
        if (aplicarDescuento) {
            descuento = (int) Math.round(subtotal * 0.20);
        }
        int precioTotal = Math.max(0, subtotal - descuento);

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
        reserva.setDescuento(descuento);
        reserva.setCuponFidelizacionAplicado(aplicarDescuento);
        reserva.setPrecioTotal(precioTotal);
        reserva.setEstado("PENDIENTE");
        reserva.setCodigoReserva(codigo);
        reserva.setGrupoRecurrente(grupoRecurrente);
        if (servicios != null && !servicios.isEmpty()) {
            reserva.setServicios(new ArrayList<>(servicios));
        }

        return reservaRepository.save(reserva);
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
            boolean aplicarDescuento = (i == 0) && fidelizacionService.tieneDescuentoDisponible(usuario);
            Reserva r = crearReservaConOpciones(usuario, cancha, fechaObjetivo, horaInicio, duracionMinutos, servicios, grupoRecurrente, aplicarDescuento);
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
            throw new IllegalStateException("Las cancelaciones solo se permiten con al menos 24 horas de anticipación.");
        }

        reserva.setEstado("CANCELADA");
        reservaRepository.save(reserva);

        pagoRepository.findByReserva(reserva).ifPresent(pago -> {
            pago.setEstado("REEMBOLSADO");
            pagoRepository.save(pago);
        });

        emailService.enviarCancelacionReserva(reserva);

        return true;
    }

    @Transactional
    public TransferenciaReserva transferirReserva(Long reservaId, Usuario solicitante, String emailDestino, String motivo) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva especificada no existe."));

        if (!reserva.getUsuario().getId().equals(solicitante.getId())) {
            throw new IllegalStateException("Solo el titular actual puede transferir esta reserva.");
        }

        if (!"CONFIRMADA".equals(reserva.getEstado())) {
            throw new IllegalStateException("Solo se pueden transferir reservas confirmadas y pagadas.");
        }

        Optional<Pago> pagoOpt = pagoRepository.findByReserva(reserva);
        if (pagoOpt.isPresent() && !"PAGADO".equals(pagoOpt.get().getEstado())) {
            throw new IllegalStateException("La reserva debe tener un pago aprobado para poder transferirse.");
        }

        LocalDateTime fechaHoraInicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        if (!fechaHoraInicio.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("No es posible transferir una reserva cuya hora de inicio ya pasó.");
        }

        if (emailDestino == null || emailDestino.trim().isEmpty()) {
            throw new IllegalArgumentException("Debes ingresar el correo electrónico del usuario destinatario.");
        }

        Usuario destino = usuarioRepository.findByEmailIgnoreCase(emailDestino.trim())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("No existe un usuario activo registrado con ese correo electrónico."));

        if (destino.getId().equals(solicitante.getId())) {
            throw new IllegalArgumentException("No puedes transferir la reserva a tu propia cuenta.");
        }

        String motivoLimpio = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : null;
        if (motivoLimpio != null && motivoLimpio.length() > 250) {
            motivoLimpio = motivoLimpio.substring(0, 250);
        }

        Usuario origen = reserva.getUsuario();
        reserva.setUsuario(destino);
        reservaRepository.save(reserva);

        TransferenciaReserva registro = new TransferenciaReserva(reserva, origen, destino, motivoLimpio);
        TransferenciaReserva guardada = transferenciaReservaRepository.save(registro);

        try {
            emailService.enviarTransferenciaReserva(reserva, origen, destino, motivoLimpio);
        } catch (Exception ignored) {
        }

        return guardada;
    }
}
