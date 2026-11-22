package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ReporteDatos;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReporteService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReporteExportacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteExportacionService exportacionService;

    public ReporteController(ReporteService reporteService, ReporteExportacionService exportacionService) {
        this.reporteService = reporteService;
        this.exportacionService = exportacionService;
    }

    @GetMapping
    public String verPanelReportes(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!esAdministrador(usuario)) {
            return "redirect:/dashboard";
        }

        desde = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        hasta = hasta == null ? LocalDate.now() : hasta;
        ReporteDatos reporte;
        try {
            reporte = reporteService.generarReportes(usuario, desde, hasta);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reportes";
        }
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("totalIngresos", reporte.totalIngresos());
        model.addAttribute("totalReservas", reporte.totalReservas());
        model.addAttribute("confirmadas", reporte.confirmadas());
        model.addAttribute("canceladas", reporte.canceladas());
        model.addAttribute("tasaOcupacion", reporte.tasaOcupacion());
        model.addAttribute("detalleCanchas", reporte.canchas());
        model.addAttribute("sinReservas", reporte.reservas().isEmpty());
        model.addAttribute("ingresosGrafico", reporte.canchas().stream()
                .sorted(Comparator.comparingLong(ReporteDatos.ResumenCancha::ingresos).reversed()).toList());
        model.addAttribute("maximoIngresos", reporte.canchas().stream()
                .mapToLong(ReporteDatos.ResumenCancha::ingresos).max().orElse(0));
        model.addAttribute("horariosDemandados", reporte.horariosDemandados());
        model.addAttribute("maximoDemanda", reporte.horariosDemandados().values().stream()
                .mapToLong(Long::longValue).max().orElse(0));
        model.addAttribute("usuarioLogueado", usuario);

        return "reportes/panel";
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session) throws IOException {
        ReporteDatos datos = datosExportacion(session, desde, hasta);
        return descarga(exportacionService.generarPdf(datos), MediaType.APPLICATION_PDF, "pdf", desde, hasta);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session) throws IOException {
        ReporteDatos datos = datosExportacion(session, desde, hasta);
        return descarga(exportacionService.generarExcel(datos),
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "xlsx", desde, hasta);
    }

    private ReporteDatos datosExportacion(HttpSession session, LocalDate desde, LocalDate hasta) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para exportar.");
        if (!esAdministrador(usuario)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        try {
            return reporteService.generarReportes(usuario, desde, hasta);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private boolean esAdministrador(Usuario usuario) {
        return usuario.isActivo() && ("ADMIN_SISTEMA".equals(usuario.getRol()) || "ADMIN_COMPLEJO".equals(usuario.getRol()));
    }

    private ResponseEntity<byte[]> descarga(byte[] contenido, MediaType tipo, String extension, LocalDate desde, LocalDate hasta) {
        return ResponseEntity.ok().contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("reporte-canchasya-" + desde + "-al-" + hasta + "." + extension).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentLength(contenido.length).body(contenido);
    }
}
