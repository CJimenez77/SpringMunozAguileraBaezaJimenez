package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.DisponibilidadService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReservaService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;
    private final ResenaRepository resenaRepository;
    private final DisponibilidadService disponibilidadService;
    private final ReservaService reservaService;
    private final WebpayService webpayService;

    public ReservaController(ReservaRepository reservaRepository,
                             CanchaRepository canchaRepository,
                             ServicioAdicionalRepository servicioAdicionalRepository,
                             ResenaRepository resenaRepository,
                             DisponibilidadService disponibilidadService,
                             ReservaService reservaService,
                             WebpayService webpayService) {
        this.reservaRepository = reservaRepository;
        this.canchaRepository = canchaRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
        this.resenaRepository = resenaRepository;
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
        this.webpayService = webpayService;
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
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
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
            Reserva reserva;
            if (recurrente && semanasRecurrencia > 1) {
                List<Reserva> reservas = reservaService.crearReservasRecurrentes(
                        usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, semanasRecurrencia
                );
                reserva = reservas.get(0);
            } else {
                reserva = reservaService.crearReserva(
                        usuario, cancha, fecha, horaInicio, duracionMinutos, servicios, null
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

        List<Reserva> reservas;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            List<Cancha> canchas = canchaRepository.findByDuenoAndActivoTrue(usuario);
            reservas = canchas.isEmpty() ? Collections.emptyList() : reservaRepository.findByCanchaInOrderByFechaDescHoraInicioDesc(canchas);
        } else if ("ADMIN_SISTEMA".equals(usuario.getRol())) {
            reservas = reservaRepository.findAll();
        } else {
            reservas = reservaRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(usuario);
        }

        model.addAttribute("reservas", reservas);
        model.addAttribute("usuarioLogueado", usuario);
        return "reservas/mis-reservas";
    }

    @GetMapping("/detalle/{id}")
    public String detalleReserva(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return "redirect:/reservas/mis-reservas";
        }

        boolean puedeVer = reserva.getUsuario().getId().equals(usuario.getId()) ||
                "ADMIN_SISTEMA".equals(usuario.getRol()) ||
                ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                        reserva.getCancha().getComplejo().getDueno() != null &&
                        reserva.getCancha().getComplejo().getDueno().getId().equals(usuario.getId()));

        if (!puedeVer) {
            return "redirect:/reservas/mis-reservas";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("usuarioLogueado", usuario);
        return "reservas/detalle";
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
            boolean cancelada = reservaService.cancelarReserva(id, usuario);
            if (cancelada) {
                redirectAttributes.addFlashAttribute("exito", "Reserva cancelada exitosamente. Se ha procesado el reembolso del pago.");
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
}
