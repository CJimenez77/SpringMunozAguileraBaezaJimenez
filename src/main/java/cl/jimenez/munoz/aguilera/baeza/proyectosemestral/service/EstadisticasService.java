package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.projection.EstadisticasProyecciones;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EstadisticasService {

    private final UsuarioRepository usuarioRepository;
    private final CanchaRepository canchaRepository;
    private final ReservaRepository reservaRepository;
    private final ResenaRepository resenaRepository;

    public EstadisticasService(UsuarioRepository usuarioRepository, CanchaRepository canchaRepository,
                               ReservaRepository reservaRepository, ResenaRepository resenaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.canchaRepository = canchaRepository;
        this.reservaRepository = reservaRepository;
        this.resenaRepository = resenaRepository;
    }

    @Transactional(readOnly = true)
    public EstadisticasDatos generar(Usuario sesion, LocalDate desde, LocalDate hasta) {
        if (sesion == null || sesion.getId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Usuario usuario = usuarioRepository.findById(sesion.getId())
                .filter(Usuario::isActivo)
                .filter(u -> "ADMIN_SISTEMA".equals(u.getRol()) || "ADMIN_COMPLEJO".equals(u.getRol()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        ReporteService.validarFechas(desde, hasta);
        if (LocalDate.MAX.equals(hasta)) throw new IllegalArgumentException("La fecha Hasta no es válida.");
        List<Cancha> autorizadas = canchaRepository.findParaReportes(usuario, "ADMIN_SISTEMA".equals(usuario.getRol()));
        if (autorizadas.isEmpty()) {
            return new EstadisticasDatos(desde, hasta, 0, 0, 0, null, 0,
                    List.of(), List.of(), List.of(), List.of(), Map.of());
        }

        List<EstadisticasDatos.CanchaRanking> canchas = reservaRepository.findRankingCanchas(autorizadas, desde, hasta)
                .stream().map(c -> new EstadisticasDatos.CanchaRanking(c.getCanchaId(), c.getNombre(), c.getComplejo(), c.getReservas())).toList();
        List<EstadisticasDatos.HoraRanking> horas = reservaRepository.findRankingHoras(autorizadas, desde, hasta)
                .stream().map(h -> new EstadisticasDatos.HoraRanking(String.format(Locale.ROOT, "%02d:00", h.getHora()), h.getReservas())).toList();
        List<EstadisticasDatos.ClienteRanking> clientes = reservaRepository.findRankingClientes(autorizadas, desde, hasta)
                .stream().map(c -> new EstadisticasDatos.ClienteRanking(c.getClienteId(), c.getNombre(), c.getEmail(), c.getReservas())).toList();

        Map<Long, EstadisticasProyecciones.CalificacionCancha> opiniones = resenaRepository
                .findCalificacionesParaEstadisticas(autorizadas, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.toMap(EstadisticasProyecciones.CalificacionCancha::getCanchaId, Function.identity()));
        List<EstadisticasDatos.CalificacionCancha> calificaciones = autorizadas.stream().map(cancha -> {
            var calificacion = opiniones.get(cancha.getId_cancha());
            long cantidad = calificacion == null ? 0 : calificacion.getOpiniones();
            Double promedio = cantidad == 0 ? null : (double) calificacion.getPuntos() / cantidad;
            return new EstadisticasDatos.CalificacionCancha(cancha.getId_cancha(), cancha.getNombre_cancha(),
                    cancha.getComplejo().getNombre_complejo(), promedio, cantidad);
        }).sorted(Comparator.comparing(EstadisticasDatos.CalificacionCancha::promedio,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingLong(EstadisticasDatos.CalificacionCancha::opiniones).reversed())
                .thenComparing(EstadisticasDatos.CalificacionCancha::nombre)
                .thenComparing(EstadisticasDatos.CalificacionCancha::id)).toList();

        long cantidadOpiniones = opiniones.values().stream().mapToLong(EstadisticasProyecciones.CalificacionCancha::getOpiniones).sum();
        long puntos = opiniones.values().stream().mapToLong(EstadisticasProyecciones.CalificacionCancha::getPuntos).sum();
        Map<String, Long> demanda = new TreeMap<>();
        horas.forEach(h -> demanda.put(h.hora(), h.reservas()));
        return new EstadisticasDatos(desde, hasta, canchas.stream().mapToLong(EstadisticasDatos.CanchaRanking::reservas).sum(),
                canchas.size(), clientes.size(), cantidadOpiniones == 0 ? null : (double) puntos / cantidadOpiniones,
                cantidadOpiniones, canchas.stream().limit(10).toList(), horas, calificaciones,
                clientes.stream().limit(10).toList(), Collections.unmodifiableMap(demanda));
    }
}
