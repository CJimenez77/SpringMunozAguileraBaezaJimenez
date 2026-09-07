package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ResenaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class ExploradorController {

    private final CanchaRepository canchaRepository;
    private final ResenaRepository resenaRepository;

    public ExploradorController(CanchaRepository canchaRepository, ResenaRepository resenaRepository) {
        this.canchaRepository = canchaRepository;
        this.resenaRepository = resenaRepository;
    }

    @GetMapping("/explorar")
    public String explorarCanchas(
            @RequestParam(value = "deporte", required = false) String deporte,
            @RequestParam(value = "busqueda", required = false) String busqueda,
            @RequestParam(value = "precioMax", required = false) Integer precioMax,
            @RequestParam(value = "calificacionMin", required = false) Integer calificacionMin,
            HttpSession session,
            Model model
    ) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        List<Cancha> todas = canchaRepository.findByActivoTrue();

        Map<Long, Double> promedios = new HashMap<>();
        Map<Long, Long> conteos = new HashMap<>();

        for (Cancha c : todas) {
            Double prom = resenaRepository.findPromedioCalificacionPorCancha(c);
            Long total = resenaRepository.countByCancha(c);
            promedios.put(c.getId_cancha(), prom != null ? Math.round(prom * 10.0) / 10.0 : 0.0);
            conteos.put(c.getId_cancha(), total != null ? total : 0L);
        }

        List<Cancha> filtradas = new ArrayList<>();
        for (Cancha c : todas) {
            if (deporte != null && !deporte.trim().isEmpty() && !deporte.equalsIgnoreCase(c.getTipo_cancha())) {
                continue;
            }
            if (precioMax != null && c.getPrecio_hora() > precioMax) {
                continue;
            }
            if (calificacionMin != null && promedios.get(c.getId_cancha()) < calificacionMin) {
                continue;
            }
            if (busqueda != null && !busqueda.trim().isEmpty()) {
                String q = busqueda.toLowerCase().trim();
                String nombreCancha = c.getNombre_cancha().toLowerCase();
                String nombreComplejo = c.getComplejo().getNombre_complejo().toLowerCase();
                String direccion = c.getComplejo().getDireccion_complejo().toLowerCase();
                if (!nombreCancha.contains(q) && !nombreComplejo.contains(q) && !direccion.contains(q)) {
                    continue;
                }
            }
            filtradas.add(c);
        }

        model.addAttribute("canchas", filtradas);
        model.addAttribute("promedios", promedios);
        model.addAttribute("conteos", conteos);
        model.addAttribute("deporteFiltro", deporte);
        model.addAttribute("busquedaFiltro", busqueda);
        model.addAttribute("precioMaxFiltro", precioMax);
        model.addAttribute("calificacionMinFiltro", calificacionMin);
        model.addAttribute("usuarioLogueado", usuario);

        return "reservas/explorar";
    }
}
