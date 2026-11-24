package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.DisponibilidadService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReservaService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;
    private final ResenaRepository resenaRepository;
    private final TransferenciaReservaRepository transferenciaReservaRepository;
    private final DisponibilidadService disponibilidadService;
    private final ReservaService reservaService;
    private final WebpayService webpayService;
    private final InvitacionReservaRepository invitacionReservaRepository;
    private final ParticipanteReservaRepository participanteReservaRepository;

    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    public ReservaController(ReservaRepository reservaRepository,
                             CanchaRepository canchaRepository,
                             ServicioAdicionalRepository servicioAdicionalRepository,
                             ResenaRepository resenaRepository,
                             TransferenciaReservaRepository transferenciaReservaRepository,
                             DisponibilidadService disponibilidadService,
                             ReservaService reservaService,
                             WebpayService webpayService,
                             InvitacionReservaRepository invitacionReservaRepository,
                             ParticipanteReservaRepository participanteReservaRepository) {
        this.reservaRepository = reservaRepository;
        this.canchaRepository = canchaRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
        this.resenaRepository = resenaRepository;
        this.transferenciaReservaRepository = transferenciaReservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
        this.webpayService = webpayService;
        this.invitacionReservaRepository = invitacionReservaRepository;
        this.participanteReservaRepository = participanteReservaRepository;
    }

    @GetMapping("/crear")
    public String formularioCrear(@RequestParam(value = "canchaId", required = false) Long canchaId,
                                  HttpSession session,
                                  Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        List<Cancha> canchas = canchaRepository.findByActivoTrue();
        Cancha canchaSeleccionada = null;

        if (canchaId != null) {
            canchaSeleccionada = canchaRepository.findById(canchaId).orElse(null);
        }
        if (canchaSeleccionada == null && !canchas.isEmpty()) {
            canchaSeleccionada = canchas.get(0);
        }

        List<ServicioAdicional> servicios = Collections.emptyList();
        if (canchaSeleccionada != null) {
            servicios = servicioAdicionalRepository.findByComplejoAndActivoTrue(canchaSeleccionada.getComplejo());
        }

        model.addAttribute("canchas", canchas);
        model.addAttribute("canchaSeleccionada", canchaSeleccionada);
        model.addAttribute("serviciosDisponibles", servicios);
        model.addAttribute("fechaHoy", LocalDate.now());
        model.addAttribute("usuarioLogueado", usuario);

        return "reservas/crear";
    }

    @GetMapping("/api/disponibilidad")
    @ResponseBody
    public ResponseEntity<List<BloqueHorario>> apiDisponibilidad(
            @RequestParam("canchaId") Long canchaId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        Cancha cancha = canchaRepository.findById(canchaId).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return ResponseEntity.notFound().build();
        }

        List<BloqueHorario> bloques = disponibilidadService.obtenerDisponibilidadDia(cancha, fecha);
        return ResponseEntity.ok(bloques);
    }

    @PostMapping({"/guardar", "/crear"})
    public String procesarReserva(
            @RequestParam("canchaId") Long canchaId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("horaInicio") String horaInicioStr,
            @RequestParam(value = "duracionMinutos", defaultValue = "60") int duracionMinutos,
            @RequestParam(value = "serviciosSeleccionados", required = false) List<Long> serviciosIds,
            @RequestParam(value = "recurrente", defaultValue = "false") boolean recurrente,
            @RequestParam(value = "semanasRecurrencia", defaultValue = "1") int semanasRecurrencia,
            @RequestParam(value = "pagoCompartido", defaultValue = "false") boolean pagoCompartido,
            @RequestParam(value = "numeroParticipantes", defaultValue = "2") int numeroParticipantes,
            @RequestParam(value = "codigoCupon", required = false) String codigoCupon,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (pagoCompartido && recurrente && semanasRecurrencia > 1) {
            redirectAttributes.addFlashAttribute("error", "El pago compartido está disponible para reservas individuales.");
            return "redirect:/reservas/crear?canchaId=" + canchaId;
        }
        if (pagoCompartido && (numeroParticipantes < 2 || numeroParticipantes > 30)) {
            redirectAttributes.addFlashAttribute("error", "Elige entre 2 y 30 participantes para dividir el pago.");
            return "redirect:/reservas/crear?canchaId=" + canchaId;
        }

        Cancha cancha = canchaRepository.findById(canchaId).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            redirectAttributes.addFlashAttribute("error", "La cancha especificada no existe.");
            return "redirect:/explorar";
        }

        LocalTime horaInicio = LocalTime.parse(horaInicioStr);
        List<ServicioAdicional> servicios = Collections.emptyList();
        if (serviciosIds != null && !serviciosIds.isEmpty()) {
            servicios = servicioAdicionalRepository.findAllById(serviciosIds);
        }

        try {
            if (pagoCompartido) {
                ParticipanteReserva titular = reservaService.crearReservaCompartida(
                        usuario, cancha, fecha, horaInicio, duracionMinutos, servicios,
                        numeroParticipantes, codigoCupon);
                return iniciarPagoParticipante(titular, model);
            }

            Reserva reserva;
            if (recurrente && semanasRecurrencia > 1) {
                List<Reserva> reservas = reservaService.crearReservasRecurrentes(
                        usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, semanasRecurrencia, codigoCupon
                );
                reserva = reservas.get(0);
            } else {
                reserva = reservaService.crearReserva(
                        usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, null, codigoCupon
                );
            }

            String buyOrder = "ORD-" + reserva.getId() + "-" + System.currentTimeMillis() % 10000;
            String sessionId = "SES-" + reserva.getId();
            double monto = reserva.getPrecioTotal();

            WebpayPlusTransactionCreateResponse response = webpayService.iniciarPago(buyOrder, sessionId, monto);

            model.addAttribute("url", response.getUrl());
            model.addAttribute("tokenWs", response.getToken());
            return "reservas/redireccion_webpay";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservas/crear?canchaId=" + canchaId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al conectar con Webpay: " + e.getMessage());
            return "redirect:/reservas/crear?canchaId=" + canchaId;
        }
    }

    @GetMapping("/mis-reservas")
    public String misReservas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        List<Reserva> baseReservas;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            List<Cancha> canchas = canchaRepository.findByDuenoAndActivoTrue(usuario);
            baseReservas = canchas.isEmpty() ? Collections.emptyList() : reservaRepository.findByCanchaInOrderByFechaDescHoraInicioDesc(canchas);
        } else if ("ADMIN_SISTEMA".equals(usuario.getRol())) {
            baseReservas = reservaRepository.findAll();
        } else {
            baseReservas = reservaRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(usuario);
        }

        List<ParticipanteReserva> participaciones = participanteReservaRepository.findByUsuarioConReserva(usuario);
        List<Reserva> reservas;
        if ("ADMIN_SISTEMA".equals(usuario.getRol())) {
            reservas = new ArrayList<>(baseReservas);
        } else {
            Map<Long, Reserva> reservasPorId = new LinkedHashMap<>();
            for (Reserva r : baseReservas) {
                reservasPorId.put(r.getId(), r);
            }
            for (ParticipanteReserva p : participaciones) {
                Reserva r = p.getInvitacion().getReserva();
                reservasPorId.putIfAbsent(r.getId(), r);
            }
            reservas = new ArrayList<>(reservasPorId.values());
        }
        reservas.sort(Comparator.comparing(Reserva::getFecha, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Reserva::getHoraInicio, Comparator.nullsLast(Comparator.reverseOrder())));

        Set<Long> cuotasCompartidasPagadas = new HashSet<>();
        Set<Long> reservasComoInvitado = new HashSet<>();
        Map<Long, String> invitacionesPorReserva = new HashMap<>();
        for (ParticipanteReserva p : participaciones) {
            InvitacionReserva inv = p.getInvitacion();
            Reserva r = inv.getReserva();
            invitacionesPorReserva.put(r.getId(), inv.getCodigo());
            if ("PAGADO".equals(p.getEstado())) {
                cuotasCompartidasPagadas.add(r.getId());
            }
            if (!r.getUsuario().getId().equals(usuario.getId())) {
                reservasComoInvitado.add(r.getId());
            }
        }

        model.addAttribute("reservas", reservas);
        model.addAttribute("transferenciasUsuario", transferenciaReservaRepository.findHistorialPorUsuario(usuario));
        model.addAttribute("cuotasCompartidasPagadas", cuotasCompartidasPagadas);
        model.addAttribute("reservasComoInvitado", reservasComoInvitado);
        model.addAttribute("invitacionesPorReserva", invitacionesPorReserva);
        model.addAttribute("usuarioLogueado", usuario);
        return "reservas/mis-reservas";
    }

    @GetMapping("/detalle/{id}")
    public String detalleReserva(@PathVariable("id") Long id, HttpSession session,
                                 Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return "redirect:/reservas/mis-reservas";
        }

        boolean esTitular = reserva.getUsuario().getId().equals(usuario.getId());
        boolean esAdmin = "ADMIN_SISTEMA".equals(usuario.getRol()) ||
                ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                        reserva.getCancha().getComplejo().getDueno() != null &&
                        reserva.getCancha().getComplejo().getDueno().getId().equals(usuario.getId()));
        InvitacionReserva invitacion = invitacionReservaRepository.findByReserva(reserva).orElse(null);
        boolean esParticipante = invitacion != null &&
                participanteReservaRepository.findByInvitacionAndUsuario(invitacion, usuario).isPresent();
        boolean puedeVer = esTitular || esAdmin || esParticipante;

        if (!puedeVer) {
            return "redirect:/reservas/mis-reservas";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("esTitular", esTitular);
        model.addAttribute("puedeGestionar", esTitular || esAdmin);
        model.addAttribute("esParticipante", esParticipante);
        model.addAttribute("transferencias", transferenciaReservaRepository.findHistorialPorReserva(reserva));
        model.addAttribute("invitacion", invitacion);
        model.addAttribute("enlaceInvitacion", invitacion == null ? "" : construirEnlaceInvitacion(invitacion.getCodigo()));
        model.addAttribute("participantes", invitacion == null ? List.of()
                : participanteReservaRepository.findByInvitacionOrderByIdAsc(invitacion));
        model.addAttribute("usuarioLogueado", usuario);
        return "reservas/detalle";
    }

    @GetMapping("/invitacion/{codigo}")
    public String verInvitacion(@PathVariable("codigo") String codigo,
                                HttpSession session,
                                Model model) {
        InvitacionReserva invitacion = invitacionReservaRepository.findByCodigo(codigo).orElse(null);
        if (invitacion == null) {
            return "redirect:/reservas/mis-reservas";
        }
        Reserva reserva = invitacion.getReserva();
        boolean reservaExpirada = !LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio())
                .isAfter(LocalDateTime.now(Reserva.ZONA_SANTIAGO));
        model.addAttribute("invitacion", invitacion);
        model.addAttribute("reservaExpirada", reservaExpirada);
        List<ParticipanteReserva> participantes =
                participanteReservaRepository.findByInvitacionOrderByIdAsc(invitacion);
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        ParticipanteReserva participanteActual = usuario == null ? null
                : participanteReservaRepository.findByInvitacionAndUsuario(invitacion, usuario).orElse(null);
        model.addAttribute("participantes", participantes);
        model.addAttribute("participanteActual", participanteActual);
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("cuotaUsuario", participanteActual == null
                ? reserva.getPrecioTotal() / invitacion.getMaxParticipantes()
                : participanteActual.getMontoCuota());
        model.addAttribute("enlaceInvitacion", construirEnlaceInvitacion(codigo));
        model.addAttribute("cuposDisponibles",
                Math.max(0, invitacion.getMaxParticipantes() - participantes.size()));
        return "reservas/invitacion";
    }

    @PostMapping("/invitacion/{codigo}/unirse")
    public String unirseAInvitacion(@PathVariable("codigo") String codigo,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        try {
            ParticipanteReserva participante = reservaService.unirseAReserva(codigo, usuario);
            return iniciarPagoParticipante(participante, model);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservas/invitacion/" + codigo;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo iniciar el pago en Webpay: " + e.getMessage());
            return "redirect:/reservas/invitacion/" + codigo;
        }
    }

    @PostMapping("/transferir/{id}")
    public String transferirReserva(@PathVariable("id") Long id,
                                    @RequestParam("emailDestino") String emailDestino,
                                    @RequestParam(value = "motivo", required = false) String motivo,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            TransferenciaReserva transferencia = reservaService.transferirReserva(id, usuario, emailDestino, motivo);
            redirectAttributes.addFlashAttribute("exito",
                    "Reserva #" + transferencia.getReserva().getCodigoReserva() +
                            " transferida exitosamente a " + transferencia.getUsuarioDestino().getNombre() +
                            " (" + transferencia.getUsuarioDestino().getEmail() + ").");
            return "redirect:/reservas/mis-reservas";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservas/detalle/" + id + "#transferir";
        }
    }

    @PostMapping("/cancelar/{id}")
    public String cancelarReserva(@PathVariable("id") Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            ReservaService.ResultadoCancelacion resultado = reservaService.cancelarReservaConDetalle(id, usuario);
            if (resultado.cancelada()) {
                String mensaje = resultado.huboReembolso()
                        ? "Reserva cancelada exitosamente. Se ha procesado el reembolso del pago."
                        : "Reserva cancelada exitosamente.";
                redirectAttributes.addFlashAttribute("exito", mensaje);
            } else {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para cancelar esta reserva.");
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/reservas/mis-reservas";
    }

    @PostMapping("/{id}/resena")
    public String calificarCancha(@PathVariable("id") Long id,
                                  @RequestParam("puntuacion") Integer puntuacion,
                                  @RequestParam("comentario") String comentario,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva != null && reserva.getUsuario().getId().equals(usuario.getId())) {
            Resena resena = new Resena(usuario, reserva.getCancha(), puntuacion, comentario);
            resenaRepository.save(resena);
            redirectAttributes.addFlashAttribute("exito", "¡Gracias por calificar la cancha!");
        }

        return "redirect:/reservas/detalle/" + id;
    }

    private String iniciarPagoParticipante(ParticipanteReserva participante, Model model) throws Exception {
        String buyOrder = reservaService.prepararPagoParticipante(participante);
        WebpayPlusTransactionCreateResponse response = webpayService.iniciarPago(
                buyOrder, "PAR-" + participante.getId(), participante.getMontoCuota());
        reservaService.registrarTokenParticipante(participante.getId(), response.getToken());
        model.addAttribute("url", response.getUrl());
        model.addAttribute("tokenWs", response.getToken());
        return "reservas/redireccion_webpay";
    }

    private String construirEnlaceInvitacion(String codigo) {
        String baseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return baseUrl + "/reservas/invitacion/" + codigo;
    }
}
