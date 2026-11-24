package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.InvitacionReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ParticipanteReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.TransferenciaReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final DisponibilidadService disponibilidadService;
    private final TarifaService tarifaService;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransferenciaReservaRepository transferenciaReservaRepository;
    private final EmailService emailService;
    private final CanchaRepository canchaRepository;
    private final FidelizacionService fidelizacionService;
    private final CuponService cuponService;
    private final InvitacionReservaRepository invitacionReservaRepository;
    private final ParticipanteReservaRepository participanteReservaRepository;
    private final WebpayService webpayService;

    public ReservaService(ReservaRepository reservaRepository,
                          DisponibilidadService disponibilidadService,
                          TarifaService tarifaService,
                          PagoRepository pagoRepository,
                          UsuarioRepository usuarioRepository,
                          TransferenciaReservaRepository transferenciaReservaRepository,
                          EmailService emailService, CanchaRepository canchaRepository,
                          FidelizacionService fidelizacionService,
                          CuponService cuponService,
                          InvitacionReservaRepository invitacionReservaRepository,
                          ParticipanteReservaRepository participanteReservaRepository,
                          WebpayService webpayService) {
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.tarifaService = tarifaService;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.transferenciaReservaRepository = transferenciaReservaRepository;
        this.emailService = emailService;
        this.canchaRepository = canchaRepository;
        this.fidelizacionService = fidelizacionService;
        this.cuponService = cuponService;
        this.invitacionReservaRepository = invitacionReservaRepository;
        this.participanteReservaRepository = participanteReservaRepository;
        this.webpayService = webpayService;
    }

    @Transactional
    public Reserva crearReserva(Usuario usuario, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
                                int duracionMinutos, List<ServicioAdicional> servicios, String grupoRecurrente) {
        return crearReserva(usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, grupoRecurrente, null);
    }

    @Transactional
    public Reserva crearReserva(Usuario usuario, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
                                int duracionMinutos, List<ServicioAdicional> servicios, String grupoRecurrente, String codigoCupon) {
        boolean aplicarDescuento = fidelizacionService.tieneDescuentoDisponible(usuario);
        return crearReservaConOpciones(usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, grupoRecurrente, aplicarDescuento, codigoCupon);
    }

    @Transactional
    public ParticipanteReserva crearReservaCompartida(Usuario usuario, Cancha cancha, LocalDate fecha,
                                                       LocalTime horaInicio, int duracionMinutos,
                                                       List<ServicioAdicional> servicios, int maxParticipantes,
                                                       String codigoCupon) {
        boolean aplicarDescuento = fidelizacionService.tieneDescuentoDisponible(usuario);
        Reserva reserva = crearReservaConOpciones(usuario, cancha, fecha, horaInicio, duracionMinutos,
                servicios, null, aplicarDescuento, codigoCupon);
        return crearInvitacionCompartida(reserva, usuario, maxParticipantes);
    }

    @Transactional
    public Reserva crearReservaConOpciones(Usuario usuario, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
                                          int duracionMinutos, List<ServicioAdicional> servicios, String grupoRecurrente,
                                          boolean aplicarDescuento, String codigoCupon) {

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
            descuento += (int) Math.round(subtotal * 0.20);
        }

        if (codigoCupon != null && !codigoCupon.trim().isEmpty()) {
            Optional<Cupon> cuponOpt = cuponService.validarCupon(codigoCupon);
            if (cuponOpt.isPresent()) {
                Cupon cupon = cuponOpt.get();
                int porcentaje = Math.min(20, Math.max(1, cupon.getPorcentaje()));
                int montoRestante = Math.max(0, subtotal - descuento);
                int descuentoCupon = (int) Math.round(montoRestante * (porcentaje / 100.0));
                descuento += descuentoCupon;
                cuponService.registrarUso(cupon);
            }
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
        return crearReservasRecurrentes(usuario, cancha, fechaInicio, horaInicio, duracionMinutos, servicios, semanasRecurrencia, null);
    }

    @Transactional
    public List<Reserva> crearReservasRecurrentes(Usuario usuario, Cancha cancha, LocalDate fechaInicio,
                                                  LocalTime horaInicio, int duracionMinutos,
                                                  List<ServicioAdicional> servicios, int semanasRecurrencia, String codigoCupon) {

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
            String cuponParaEsta = (i == 0) ? codigoCupon : null;
            Reserva r = crearReservaConOpciones(usuario, cancha, fechaObjetivo, horaInicio, duracionMinutos, servicios, grupoRecurrente, aplicarDescuento, cuponParaEsta);
            creadas.add(r);
        }

        return creadas;
    }

    public record ResultadoCancelacion(boolean cancelada, boolean huboReembolso) {}

    public boolean cancelarReserva(Long reservaId, Usuario solicitante) {
        return cancelarReservaConDetalle(reservaId, solicitante).cancelada();
    }

    public ResultadoCancelacion cancelarReservaConDetalle(Long reservaId, Usuario solicitante) {
        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return new ResultadoCancelacion(false, false);
        }

        boolean esPropietario = reserva.getUsuario().getId().equals(solicitante.getId());
        boolean esAdmin = "ADMIN_SISTEMA".equals(solicitante.getRol()) ||
                ("ADMIN_COMPLEJO".equals(solicitante.getRol()) &&
                        reserva.getCancha().getComplejo().getDueno() != null &&
                        reserva.getCancha().getComplejo().getDueno().getId().equals(solicitante.getId()));

        if (!esPropietario && !esAdmin) {
            return new ResultadoCancelacion(false, false);
        }

        boolean reintento = "CANCELACION_PENDIENTE".equals(reserva.getEstado());
        if (!reintento && !reserva.esCancelable()) {
            throw new IllegalStateException("La reserva solo se puede cancelar antes de su hora de inicio.");
        }

        int porcentaje = reintento ? reserva.getPorcentajeReembolsoAplicado() : reserva.porcentajeReembolso();
        if (reservaRepository.iniciarCancelacion(reservaId, porcentaje) != 1) {
            throw new IllegalStateException("La cancelación ya está en proceso o la reserva cambió de estado.");
        }
        reserva.setEstado("CANCELACION_EN_PROCESO");
        reserva.setPorcentajeReembolsoAplicado(porcentaje);

        boolean huboReembolso = false;
        InvitacionReserva invitacion = null;
        List<ParticipanteReserva> participantes = List.of();
        try {
            invitacion = invitacionReservaRepository.findByReserva(reserva).orElse(null);
            if (invitacion != null) {
                invitacion.setActiva(false);
                invitacionReservaRepository.save(invitacion);
            }
            Optional<Pago> pagoOpt = pagoRepository.findByReserva(reserva);
            if (pagoOpt.isPresent()) {
                Pago pago = pagoOpt.get();
                if ("PAGADO".equals(pago.getEstado())
                        || (reintento && "REEMBOLSO_EN_PROCESO".equals(pago.getEstado()))) {
                    reembolsarPago(pago, porcentaje);
                    if (pago.getMontoReembolsado() != null && pago.getMontoReembolsado() > 0) {
                        huboReembolso = true;
                    }
                } else if ("REEMBOLSO_EN_PROCESO".equals(pago.getEstado())) {
                    throw new IllegalStateException("El reembolso del pago está pendiente de conciliación con Webpay.");
                }
            }
            if (invitacion != null) {
                participantes = participanteReservaRepository.findByInvitacionOrderByIdAsc(invitacion);
                for (ParticipanteReserva participante : participantes) {
                    if ("PAGADO".equals(participante.getEstado())
                            || (reintento && "REEMBOLSO_EN_PROCESO".equals(participante.getEstado()))) {
                        reembolsarParticipante(participante, porcentaje);
                        if (participante.getMontoReembolsado() != null && participante.getMontoReembolsado() > 0) {
                            huboReembolso = true;
                        }
                    } else if ("REEMBOLSO_EN_PROCESO".equals(participante.getEstado())) {
                        throw new IllegalStateException("Un reembolso de participante está pendiente de conciliación con Webpay.");
                    }
                }
            }
        } catch (Exception e) {
            reservaRepository.cambiarEstadoCancelacion(reservaId, "CANCELACION_EN_PROCESO",
                    "CANCELACION_PENDIENTE");
            reserva.setEstado("CANCELACION_PENDIENTE");
            throw new IllegalStateException("La cancelación quedó pendiente: " + e.getMessage(), e);
        }

        if (reservaRepository.cambiarEstadoCancelacion(reservaId, "CANCELACION_EN_PROCESO", "CANCELADA") != 1) {
            reservaRepository.cambiarEstadoCancelacion(reservaId, "CANCELACION_EN_PROCESO",
                    "CANCELACION_PENDIENTE");
            reserva.setEstado("CANCELACION_PENDIENTE");
            throw new IllegalStateException("No se pudo finalizar la cancelación. Revisa el estado de los reembolsos.");
        }
        reserva.setEstado("CANCELADA");

        try {
            emailService.enviarCancelacionReserva(reserva, huboReembolso);
            notificarParticipantesCancelacion(reserva, participantes, huboReembolso);
        } catch (Exception e) {
            log.warn("Reserva {} cancelada, pero falló el correo de notificación",
                    reserva.getId(), e);
        }

        return new ResultadoCancelacion(true, huboReembolso);
    }

    private void notificarParticipantesCancelacion(Reserva reserva, List<ParticipanteReserva> participantes,
                                                   boolean huboReembolso) {
        if (participantes == null || participantes.isEmpty()) {
            return;
        }
        Long titularId = reserva.getUsuario() != null ? reserva.getUsuario().getId() : null;
        String emailTitular = reserva.getUsuario() != null ? reserva.getUsuario().getEmail() : null;
        for (ParticipanteReserva participante : participantes) {
            Usuario u = participante.getUsuario();
            if (u != null && u.getEmail() != null && !u.getEmail().isBlank()
                    && !Objects.equals(u.getId(), titularId)
                    && (emailTitular == null || !u.getEmail().equalsIgnoreCase(emailTitular))) {
                emailService.enviarCancelacionReserva(reserva, u, huboReembolso);
            }
        }
    }

    @Transactional
    public ParticipanteReserva crearInvitacionCompartida(Reserva reserva, Usuario titular, int maxParticipantes) {
        if (maxParticipantes < 2 || maxParticipantes > 30) {
            throw new IllegalArgumentException("La cantidad de participantes debe estar entre 2 y 30.");
        }
        if (reserva.getPrecioTotal() < maxParticipantes) {
            throw new IllegalArgumentException("El total debe ser al menos de $"
                    + maxParticipantes + " CLP para dividirlo entre los participantes.");
        }

        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        invitacion.setCodigo(UUID.randomUUID().toString().replace("-", ""));
        invitacion.setMaxParticipantes(maxParticipantes);
        invitacion.setActiva(true);
        invitacion = invitacionReservaRepository.save(invitacion);

        int cuotaBase = reserva.getPrecioTotal() / maxParticipantes;
        int resto = reserva.getPrecioTotal() % maxParticipantes;
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setInvitacion(invitacion);
        participante.setUsuario(titular);
        participante.setMontoCuota(cuotaBase + resto);
        return participanteReservaRepository.save(participante);
    }

    @Transactional
    public ParticipanteReserva unirseAReserva(String codigo, Usuario usuario) {
        InvitacionReserva invitacion = invitacionReservaRepository.findByCodigoForUpdate(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de invitación no es válido."));
        Reserva reserva = invitacion.getReserva();
        if (!invitacion.isActiva() || !"PENDIENTE".equals(reserva.getEstado())
                || !LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio()).isAfter(LocalDateTime.now(Reserva.ZONA_SANTIAGO))) {
            throw new IllegalStateException("Esta invitación ya no está disponible.");
        }
        Optional<ParticipanteReserva> participanteExistente =
                participanteReservaRepository.findByInvitacionAndUsuario(invitacion, usuario);
        if (participanteExistente.isPresent()) {
            if ("PENDIENTE".equals(participanteExistente.get().getEstado())) {
                return participanteExistente.get();
            }
            throw new IllegalStateException("Ya pagaste tu cuota para esta reserva.");
        }
        if (participanteReservaRepository.countByInvitacion(invitacion) >= invitacion.getMaxParticipantes()) {
            throw new IllegalStateException("La reserva ya alcanzó el máximo de participantes.");
        }

        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setInvitacion(invitacion);
        participante.setUsuario(usuario);
        participante.setMontoCuota(reserva.getPrecioTotal() / invitacion.getMaxParticipantes());
        return participanteReservaRepository.save(participante);
    }

    @Transactional
    public String prepararPagoParticipante(ParticipanteReserva participante) {
        if (!"PENDIENTE".equals(participante.getEstado())) {
            throw new IllegalStateException("La cuota de este participante ya fue procesada.");
        }
        Reserva reserva = participante.getInvitacion() != null ? participante.getInvitacion().getReserva() : null;
        if (reserva != null && !"PENDIENTE".equals(reserva.getEstado())) {
            throw new IllegalStateException("La reserva ya no está pendiente de pago.");
        }
        String buyOrder = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        participante.setTokenWs(null);
        participante.setBuyOrder(buyOrder);
        participanteReservaRepository.save(participante);
        return buyOrder;
    }

    @Transactional
    public void registrarTokenParticipante(Long participanteId, String tokenWs) {
        ParticipanteReserva participante = participanteReservaRepository.findById(participanteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el participante."));
        participante.setTokenWs(tokenWs);
        participanteReservaRepository.save(participante);
    }

    @Transactional
    public void limpiarIntentoPagoParticipante(String tokenWs) {
        participanteReservaRepository.findByTokenWs(tokenWs).ifPresent(participante -> {
            if ("PENDIENTE".equals(participante.getEstado())) {
                participante.setTokenWs(null);
                participante.setBuyOrder(null);
                participanteReservaRepository.save(participante);
            }
        });
    }

    @Transactional
    public boolean confirmarPagoParticipante(ParticipanteReserva participante, String buyOrder, String tokenWs,
                                              double montoConfirmado) {
        if (participante.getBuyOrder() == null || !Objects.equals(buyOrder, participante.getBuyOrder())
                || !Objects.equals(tokenWs, participante.getTokenWs())) {
            throw new IllegalStateException("La transacción no coincide con la cuota pendiente.");
        }
        if (Math.round(montoConfirmado) != participante.getMontoCuota()) {
            throw new IllegalStateException("El monto confirmado por Webpay no coincide con la cuota.");
        }
        if ("PAGADO".equals(participante.getEstado())) {
            return "CONFIRMADA".equals(participante.getInvitacion().getReserva().getEstado());
        }
        if ("REEMBOLSADO".equals(participante.getEstado())
                || "REEMBOLSO_PARCIAL".equals(participante.getEstado())) {
            return false;
        }
        if ("REEMBOLSO_EN_PROCESO".equals(participante.getEstado())) {
            throw new IllegalStateException("El reembolso de esta cuota está pendiente de conciliación con Webpay.");
        }

        InvitacionReserva invitacion = invitacionReservaRepository
                .findByCodigoForUpdate(participante.getInvitacion().getCodigo())
                .orElseThrow(() -> new IllegalStateException("La invitación ya no está disponible."));
        Reserva reserva = invitacion.getReserva();
        if ("CANCELADA".equals(reserva.getEstado()) || reserva.getEstado().startsWith("CANCELACION_")) {
            int porcentaje = reserva.getPorcentajeReembolsoAplicado();
            int montoReembolso = calcularMontoReembolso(participante.getMontoCuota(), porcentaje);
            if (montoReembolso > 0) {
                try {
                    webpayService.reembolsar(tokenWs, montoReembolso);
                } catch (Exception e) {
                    throw new IllegalStateException("El pago se aprobó después de cancelar, pero no se pudo reversar: "
                            + e.getMessage(), e);
                }
            }
            participante.setEstado(porcentaje == 100 ? "REEMBOLSADO" : "REEMBOLSO_PARCIAL");
            participante.setMontoReembolsado(montoReembolso);
            participante.setTransaccionId(tokenWs);
            participante.setFechaPago(LocalDateTime.now());
            participanteReservaRepository.save(participante);
            return false;
        }

        participante.setEstado("PAGADO");
        participante.setTransaccionId(tokenWs);
        participante.setFechaPago(LocalDateTime.now());
        participanteReservaRepository.save(participante);

        List<ParticipanteReserva> participantes = participanteReservaRepository
                .findByInvitacionOrderByIdAsc(invitacion);
        boolean todosPagados = participantes.size() == invitacion.getMaxParticipantes()
                && participantes.stream().allMatch(p -> "PAGADO".equals(p.getEstado()));
        if (todosPagados) {
            reserva.setEstado("CONFIRMADA");
            reservaRepository.save(reserva);
            invitacion.setActiva(false);
            invitacionReservaRepository.save(invitacion);
            try {
                emailService.enviarConfirmacionReserva(reserva);
                Long titularId = reserva.getUsuario() != null ? reserva.getUsuario().getId() : null;
                String emailTitular = reserva.getUsuario() != null ? reserva.getUsuario().getEmail() : null;
                for (ParticipanteReserva p : participantes) {
                    Usuario u = p.getUsuario();
                    if (u != null && u.getEmail() != null && !u.getEmail().isBlank()
                            && !Objects.equals(u.getId(), titularId)
                            && (emailTitular == null || !u.getEmail().equalsIgnoreCase(emailTitular))) {
                        emailService.enviarConfirmacionReserva(reserva, u);
                    }
                }
            } catch (Exception e) {
                log.warn("Reserva {} confirmada, pero falló el correo de notificación", reserva.getId(), e);
            }
        }
        return todosPagados;
    }

    private void reembolsarPago(Pago pago, int porcentaje) {
        int montoReembolso = calcularMontoReembolso(pago.getMonto(), porcentaje);
        int reclamada = pagoRepository.iniciarReembolso(pago.getId(), montoReembolso);
        if (reclamada != 1) {
            String estadoActual = pagoRepository.findById(pago.getId()).map(Pago::getEstado).orElse("DESCONOCIDO");
            if ("REEMBOLSADO".equals(estadoActual) || "REEMBOLSO_PARCIAL".equals(estadoActual)) {
                return;
            }
            throw new IllegalStateException("El pago ya tiene un reembolso en proceso y requiere conciliación.");
        }
        pago.setEstado("REEMBOLSO_EN_PROCESO");
        pago.setMontoReembolsado(montoReembolso);
        boolean esWebpay = pago.getMetodoPago() == null
                || pago.getMetodoPago().toUpperCase().contains("WEBPAY");
        boolean tieneTransaccion = pago.getTransaccionId() != null && !pago.getTransaccionId().isBlank();
        try {
            if (montoReembolso > 0 && esWebpay && tieneTransaccion) {
                webpayService.reembolsar(pago.getTransaccionId(), montoReembolso);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo confirmar el reverso del pago original: " + e.getMessage(), e);
        }
        pago.setMontoReembolsado(montoReembolso);
        pago.setEstado(porcentaje == 100 ? "REEMBOLSADO" : "REEMBOLSO_PARCIAL");
        pagoRepository.save(pago);
    }

    private void reembolsarParticipante(ParticipanteReserva participante, int porcentaje) {
        int montoReembolso = calcularMontoReembolso(participante.getMontoCuota(), porcentaje);
        int reclamada = participanteReservaRepository.iniciarReembolso(participante.getId(), montoReembolso);
        if (reclamada != 1) {
            String estadoActual = participanteReservaRepository.findById(participante.getId())
                    .map(ParticipanteReserva::getEstado).orElse("DESCONOCIDO");
            if ("REEMBOLSADO".equals(estadoActual) || "REEMBOLSO_PARCIAL".equals(estadoActual)) {
                return;
            }
            throw new IllegalStateException("La cuota ya tiene un reembolso en proceso y requiere conciliación.");
        }
        participante.setEstado("REEMBOLSO_EN_PROCESO");
        participante.setMontoReembolsado(montoReembolso);
        String tokenReembolso = participante.getTokenWs() != null && !participante.getTokenWs().isBlank()
                ? participante.getTokenWs()
                : participante.getTransaccionId();
        try {
            if (montoReembolso > 0 && tokenReembolso != null && !tokenReembolso.isBlank()) {
                webpayService.reembolsar(tokenReembolso, montoReembolso);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo confirmar el reverso de una cuota: " + e.getMessage(), e);
        }
        participante.setMontoReembolsado(montoReembolso);
        participante.setEstado(porcentaje == 100 ? "REEMBOLSADO" : "REEMBOLSO_PARCIAL");
        participanteReservaRepository.save(participante);
    }

    private int calcularMontoReembolso(int monto, int porcentaje) {
        return (int) Math.floor((long) monto * porcentaje / 100.0);
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
