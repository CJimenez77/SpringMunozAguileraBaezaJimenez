package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.EquipoJugador;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.EquipoJugadorRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/reservas/{reservaId}/equipos")
public class EquipoController {

    private final ReservaRepository reservaRepository;
    private final EquipoJugadorRepository equipoJugadorRepository;

    public EquipoController(ReservaRepository reservaRepository, EquipoJugadorRepository equipoJugadorRepository) {
        this.reservaRepository = reservaRepository;
        this.equipoJugadorRepository = equipoJugadorRepository;
    }

    @GetMapping
    public String verEquipos(@PathVariable("reservaId") Long reservaId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return "redirect:/reservas/mis-reservas";
        }

        List<EquipoJugador> equipoA = equipoJugadorRepository.findByReservaAndEquipo(reserva, "A");
        List<EquipoJugador> equipoB = equipoJugadorRepository.findByReservaAndEquipo(reserva, "B");

        String tipoCancha = reserva.getCancha() != null ? reserva.getCancha().getTipo_cancha() : "";
        List<String> posiciones = obtenerPosicionesPorDeporte(tipoCancha);
        String deporteClase = obtenerClaseCancha(tipoCancha);

        model.addAttribute("reserva", reserva);
        model.addAttribute("equipoA", equipoA);
        model.addAttribute("equipoB", equipoB);
        model.addAttribute("nuevoJugador", new EquipoJugador());
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("posiciones", posiciones);
        model.addAttribute("deporteClase", deporteClase);

        return "reservas/equipos";
    }

    @PostMapping("/agregar")
    public String agregarJugador(@PathVariable("reservaId") Long reservaId,
                                 @ModelAttribute EquipoJugador nuevoJugador,
                                 HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return "redirect:/reservas/mis-reservas";
        }

        nuevoJugador.setReserva(reserva);
        equipoJugadorRepository.save(nuevoJugador);

        return "redirect:/reservas/" + reservaId + "/equipos";
    }

    @GetMapping("/eliminar/{jugadorId}")
    public String eliminarJugador(@PathVariable("reservaId") Long reservaId,
                                  @PathVariable("jugadorId") Long jugadorId,
                                  HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        EquipoJugador jugador = equipoJugadorRepository.findById(jugadorId).orElse(null);
        if (jugador != null && jugador.getReserva().getId().equals(reservaId)) {
            equipoJugadorRepository.delete(jugador);
        }

        return "redirect:/reservas/" + reservaId + "/equipos";
    }

    public List<String> obtenerPosicionesPorDeporte(String tipoCancha) {
        if (tipoCancha == null) {
            return List.of("Titular", "Suplente");
        }
        String normalizado = java.text.Normalizer.normalize(tipoCancha, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();

        if (normalizado.contains("padel")) {
            return List.of("Drive (Derecha)", "Revés (Izquierda)");
        } else if (normalizado.contains("tenis")) {
            return List.of("Singles", "Dobles (Drive)", "Dobles (Revés)");
        } else if (normalizado.contains("futbol") || normalizado.contains("baby")) {
            return List.of("Arquero", "Defensa", "Volante", "Delantero");
        } else if (normalizado.contains("basquet") || normalizado.contains("baloncesto")) {
            return List.of("Base", "Escolta", "Alero", "Ala-Pívot", "Pívot");
        } else if (normalizado.contains("volei") || normalizado.contains("voley")) {
            return List.of("Armador / Colocador", "Opuesto", "Punta Receptor", "Central", "Líbero");
        }
        return List.of("Titular", "Suplente");
    }

    public String obtenerClaseCancha(String tipoCancha) {
        if (tipoCancha == null) {
            return "pitch-futbol";
        }
        String normalizado = java.text.Normalizer.normalize(tipoCancha, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();

        if (normalizado.contains("padel")) {
            return "pitch-padel";
        } else if (normalizado.contains("tenis")) {
            return "pitch-tenis";
        } else if (normalizado.contains("basquet") || normalizado.contains("baloncesto")) {
            return "pitch-basquetbol";
        }
        return "pitch-futbol";
    }
}
