package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ServicioAdicional;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ServicioAdicionalRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/complejos")
public class ComplejoController {

    private final ComplejoRepositorio complejoRepositorio;
    private final ServicioAdicionalRepository servicioAdicionalRepository;

    public ComplejoController(ComplejoRepositorio complejoRepositorio,
                              ServicioAdicionalRepository servicioAdicionalRepository) {
        this.complejoRepositorio = complejoRepositorio;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
    }

    @GetMapping
    public String listarComplejos(HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) {
            return "redirect:/login";
        }
        List<Complejo> lista;
        if ("ADMIN_COMPLEJO".equals(logueado.getRol())) {
            lista = complejoRepositorio.findByDuenoAndActivoTrue(logueado);
        } else {
            lista = complejoRepositorio.findByActivoTrue();
        }
        model.addAttribute("complejos", lista);
        model.addAttribute("usuarioLogueado", logueado);
        return "complejos/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        model.addAttribute("complejo", new Complejo());
        return "complejos/formulario";
    }

    @PostMapping("/guardar")
    public String guardarComplejo(@ModelAttribute Complejo complejo,
                                  @RequestParam(value = "archivoFoto", required = false) MultipartFile foto,
                                  HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/dashboard";
        }

        try {
            String fotoBase64 = null;
            if (foto != null && !foto.isEmpty()) {
                String encoded = Base64.getEncoder().encodeToString(foto.getBytes());
                fotoBase64 = "data:" + foto.getContentType() + ";base64," + encoded;
            }

            if (complejo.getId_complejo() != null) {
                Complejo existente = complejoRepositorio.findById(complejo.getId_complejo()).orElse(null);
                if (existente == null) {
                    return "redirect:/complejos";
                }
                if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                        (existente.getDueno() == null || !logueado.getId().equals(existente.getDueno().getId()))) {
                    return "redirect:/complejos";
                }
                existente.setNombre_complejo(complejo.getNombre_complejo());
                existente.setDireccion_complejo(complejo.getDireccion_complejo());
                existente.setUbicacionMapa(complejo.getUbicacionMapa());
                if (fotoBase64 != null) {
                    existente.setFoto(fotoBase64);
                }
                complejoRepositorio.save(existente);
                return "redirect:/complejos";
            } else {
                complejo.setDueno(logueado);
                complejo.setActivo(true);
                if (fotoBase64 != null) {
                    complejo.setFoto(fotoBase64);
                }
                complejoRepositorio.save(complejo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/complejos";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            return "redirect:/complejos";
        }
        if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                (complejo.getDueno() == null || !logueado.getId().equals(complejo.getDueno().getId()))) {
            return "redirect:/complejos";
        }
        model.addAttribute("complejo", complejo);
        return "complejos/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarComplejo(@PathVariable("id") Long id, HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo != null) {
            if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                    (complejo.getDueno() == null || !logueado.getId().equals(complejo.getDueno().getId()))) {
                return "redirect:/complejos";
            }
            complejo.setActivo(false);
            complejoRepositorio.save(complejo);
        }
        return "redirect:/complejos";
    }

    @GetMapping("/{id}/servicios")
    public String gestionarServicios(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            return "redirect:/complejos";
        }
        if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                (complejo.getDueno() == null || !logueado.getId().equals(complejo.getDueno().getId()))) {
            return "redirect:/complejos";
        }

        model.addAttribute("complejo", complejo);
        model.addAttribute("servicios", servicioAdicionalRepository.findByComplejoAndActivoTrue(complejo));
        model.addAttribute("nuevoServicio", new ServicioAdicional());
        model.addAttribute("usuarioLogueado", logueado);
        return "complejos/servicios";
    }

    @PostMapping("/{id}/servicios/guardar")
    public String guardarServicio(@PathVariable("id") Long id,
                                  @ModelAttribute ServicioAdicional nuevoServicio,
                                  HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            return "redirect:/complejos";
        }
        if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                (complejo.getDueno() == null || !logueado.getId().equals(complejo.getDueno().getId()))) {
            return "redirect:/complejos";
        }

        nuevoServicio.setComplejo(complejo);
        nuevoServicio.setActivo(true);
        nuevoServicio.setId(null); // Obliga a Spring Boot a crear un nuevo registro

        servicioAdicionalRepository.save(nuevoServicio);
        return "redirect:/complejos/" + id + "/servicios";
    }

    @GetMapping("/{id}/servicios/eliminar/{idServicio}")
    public String eliminarServicio(@PathVariable("id") Long id,
                                   @PathVariable("idServicio") Long idServicio,
                                   HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        ServicioAdicional servicio = servicioAdicionalRepository.findById(idServicio).orElse(null);
        if (servicio != null && servicio.getComplejo().getId_complejo().equals(id)) {
            servicio.setActivo(false);
            servicioAdicionalRepository.save(servicio);
        }
        return "redirect:/complejos/" + id + "/servicios";
    }
}