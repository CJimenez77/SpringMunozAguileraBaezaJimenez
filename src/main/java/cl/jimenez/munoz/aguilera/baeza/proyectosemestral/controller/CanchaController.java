package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.HorarioCanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.TarifaFranjaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.DisponibilidadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/canchas")
public class CanchaController {

    private final CanchaRepository canchaRepository;
    private final ComplejoRepositorio complejoRepositorio;
    private final HorarioCanchaRepository horarioCanchaRepository;
    private final TarifaFranjaRepository tarifaFranjaRepository;
    private final DisponibilidadService disponibilidadService;

    public CanchaController(CanchaRepository canchaRepository,
                            ComplejoRepositorio complejoRepositorio,
                            HorarioCanchaRepository horarioCanchaRepository,
                            TarifaFranjaRepository tarifaFranjaRepository,
                            DisponibilidadService disponibilidadService) {
        this.canchaRepository = canchaRepository;
        this.complejoRepositorio = complejoRepositorio;
        this.horarioCanchaRepository = horarioCanchaRepository;
        this.tarifaFranjaRepository = tarifaFranjaRepository;
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public String listarCanchas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        List<Cancha> canchas;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            canchas = canchaRepository.findByDuenoAndActivoTrue(usuario);
        } else {
            canchas = canchaRepository.findByActivoTrue();
        }

        model.addAttribute("canchas", canchas);
        model.addAttribute("usuarioLogueado", usuario);
        return "canchas/lista";
    }

    @GetMapping("/complejo/{idComplejo}")
    public String listarCanchasPorComplejo(@PathVariable("idComplejo") Long idComplejo, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        Complejo complejo = complejoRepositorio.findById(idComplejo).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            return "redirect:/canchas";
        }

        if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                (complejo.getDueno() == null || !complejo.getDueno().getId().equals(usuario.getId()))) {
            return "redirect:/canchas";
        }

        model.addAttribute("canchas", canchaRepository.findByComplejoIdAndActivoTrue(idComplejo));
        model.addAttribute("complejoFiltro", complejo);
        model.addAttribute("usuarioLogueado", usuario);
        return "canchas/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        List<Complejo> complejosDisponibles;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            complejosDisponibles = complejoRepositorio.findByDuenoAndActivoTrue(usuario);
        } else {
            complejosDisponibles = complejoRepositorio.findByActivoTrue();
        }

        model.addAttribute("cancha", new Cancha());
        model.addAttribute("complejos", complejosDisponibles);
        return "canchas/formulario";
    }

    @PostMapping("/guardar")
    public String guardarCancha(@ModelAttribute Cancha cancha,
                                @RequestParam(value = "archivoFoto", required = false) MultipartFile foto,
                                HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        if (cancha.getComplejo() != null) {
            Complejo comp = complejoRepositorio.findById(cancha.getComplejo().getId_complejo()).orElse(null);
            if (comp == null || !comp.isActivo()) return "redirect:/canchas";
            if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                    (comp.getDueno() == null || !comp.getDueno().getId().equals(usuario.getId()))) {
                return "redirect:/canchas";
            }
            cancha.setComplejo(comp);
        }

        try {
            String fotoBase64 = null;
            if (foto != null && !foto.isEmpty()) {
                String encoded = Base64.getEncoder().encodeToString(foto.getBytes());
                fotoBase64 = "data:" + foto.getContentType() + ";base64," + encoded;
            }

            if (cancha.getId_cancha() != null) {
                Cancha existente = canchaRepository.findById(cancha.getId_cancha()).orElse(null);
                if (existente != null) {
                    if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                            (existente.getComplejo().getDueno() == null || !existente.getComplejo().getDueno().getId().equals(usuario.getId()))) {
                        return "redirect:/canchas";
                    }
                    existente.setNombre_cancha(cancha.getNombre_cancha());
                    existente.setTipo_cancha(cancha.getTipo_cancha());
                    existente.setPrecio_hora(cancha.getPrecio_hora());
                    existente.setComplejo(cancha.getComplejo());
                    if (fotoBase64 != null) {
                        existente.setFoto(fotoBase64);
                    }
                    canchaRepository.save(existente);
                    return "redirect:/canchas";
                }
            }

            cancha.setActivo(true);
            if (fotoBase64 != null) {
                cancha.setFoto(fotoBase64);
            }
            Cancha guardada = canchaRepository.save(cancha);
            disponibilidadService.obtenerHorariosSemanales(guardada);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/canchas";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return "redirect:/canchas";
        }

        if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                (cancha.getComplejo().getDueno() == null || !cancha.getComplejo().getDueno().getId().equals(usuario.getId()))) {
            return "redirect:/canchas";
        }

        List<Complejo> complejosDisponibles;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            complejosDisponibles = complejoRepositorio.findByDuenoAndActivoTrue(usuario);
        } else {
            complejosDisponibles = complejoRepositorio.findByActivoTrue();
        }

        model.addAttribute("cancha", cancha);
        model.addAttribute("complejos", complejosDisponibles);
        return "canchas/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCancha(@PathVariable("id") Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha != null) {
            if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                    (cancha.getComplejo().getDueno() == null || !cancha.getComplejo().getDueno().getId().equals(usuario.getId()))) {
                return "redirect:/canchas";
            }
            cancha.setActivo(false);
            canchaRepository.save(cancha);
        }
        return "redirect:/canchas";
    }

    @GetMapping("/{id}/horarios")
    public String verHorarios(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return "redirect:/canchas";
        }

        List<HorarioCancha> horarios = disponibilidadService.obtenerHorariosSemanales(cancha);
        model.addAttribute("cancha", cancha);
        model.addAttribute("horarios", horarios);
        model.addAttribute("usuarioLogueado", usuario);
        return "canchas/horarios";
    }

    @PostMapping("/{id}/horarios/guardar")
    public String guardarHorario(@PathVariable("id") Long id,
                                 @RequestParam("diaSemana") Integer diaSemana,
                                 @RequestParam("horaApertura") String horaApertura,
                                 @RequestParam("horaCierre") String horaCierre,
                                 @RequestParam(value = "abierto", defaultValue = "false") boolean abierto,
                                 HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return "redirect:/canchas";
        }

        HorarioCancha horario = horarioCanchaRepository.findByCanchaAndDiaSemana(cancha, diaSemana)
                .orElse(new HorarioCancha(cancha, diaSemana, LocalTime.of(8, 0), LocalTime.of(23, 0), true));

        horario.setHoraApertura(LocalTime.parse(horaApertura));
        horario.setHoraCierre(LocalTime.parse(horaCierre));
        horario.setAbierto(abierto);
        horarioCanchaRepository.save(horario);

        return "redirect:/canchas/" + id + "/horarios";
    }

    @GetMapping("/{id}/tarifas")
    public String verTarifas(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return "redirect:/canchas";
        }

        List<TarifaFranja> tarifas = tarifaFranjaRepository.findByCanchaAndActivoTrue(cancha);
        model.addAttribute("cancha", cancha);
        model.addAttribute("tarifas", tarifas);
        model.addAttribute("nuevaTarifa", new TarifaFranja());
        model.addAttribute("usuarioLogueado", usuario);
        return "canchas/tarifas";
    }

    @PostMapping("/{id}/tarifas/guardar")
    public String guardarTarifa(@PathVariable("id") Long id,
                                @ModelAttribute TarifaFranja nuevaTarifa,
                                HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return "redirect:/canchas";
        }

        nuevaTarifa.setCancha(cancha);
        nuevaTarifa.setActivo(true);
        tarifaFranjaRepository.save(nuevaTarifa);

        return "redirect:/canchas/" + id + "/tarifas";
    }

    @GetMapping("/{id}/tarifas/eliminar/{idTarifa}")
    public String eliminarTarifa(@PathVariable("id") Long id,
                                 @PathVariable("idTarifa") Long idTarifa,
                                 HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        TarifaFranja tarifa = tarifaFranjaRepository.findById(idTarifa).orElse(null);
        if (tarifa != null && tarifa.getCancha().getId_cancha().equals(id)) {
            tarifa.setActivo(false);
            tarifaFranjaRepository.save(tarifa);
        }

        return "redirect:/canchas/" + id + "/tarifas";
    }
}