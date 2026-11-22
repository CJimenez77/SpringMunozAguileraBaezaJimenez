package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioCanchaRepository horarioRepository;
    private final BloqueoHorarioRepository bloqueoRepository;

    public ReporteService(ReservaRepository reservaRepository, CanchaRepository canchaRepository,
                          UsuarioRepository usuarioRepository, HorarioCanchaRepository horarioRepository,
                          BloqueoHorarioRepository bloqueoRepository) {
        this.reservaRepository = reservaRepository;
        this.canchaRepository = canchaRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioRepository = horarioRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    public static void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha Desde debe ser anterior o igual a Hasta.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= 366) {
            throw new IllegalArgumentException("Selecciona un rango de hasta 366 días para generar el reporte.");
        }
    }

    @Transactional(readOnly = true)
    public ReporteDatos generarReportes(Usuario sesion, LocalDate desde, LocalDate hasta) {
        if (sesion == null || sesion.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarioRepository.findById(sesion.getId())
                .filter(Usuario::isActivo)
                .filter(u -> "ADMIN_COMPLEJO".equals(u.getRol()) || "ADMIN_SISTEMA".equals(u.getRol()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        validarFechas(desde, hasta);
        boolean todos = "ADMIN_SISTEMA".equals(usuario.getRol());
        List<Cancha> canchas = canchaRepository.findParaReportes(usuario, todos);
        List<Reserva> reservas = reservaRepository.findParaReportes(usuario, todos, desde, hasta);
        Map<Long, List<Reserva>> porCancha = reservas.stream()
                .collect(Collectors.groupingBy(r -> r.getCancha().getId_cancha()));
        Map<Long, List<HorarioCancha>> horarios = canchas.isEmpty() ? Map.of()
                : horarioRepository.findByCanchaIn(canchas).stream()
                .collect(Collectors.groupingBy(h -> h.getCancha().getId_cancha()));
        Map<Long, List<BloqueoHorario>> bloqueos = canchas.isEmpty() ? Map.of()
                : bloqueoRepository.findParaReportes(canchas, desde, hasta).stream()
                .collect(Collectors.groupingBy(b -> b.getCancha().getId_cancha()));

        List<ReporteDatos.ResumenCancha> resumen = new ArrayList<>();
        for (Cancha cancha : canchas) {
            List<Reserva> deCancha = porCancha.getOrDefault(cancha.getId_cancha(), List.of());
            Map<LocalDate, List<Reserva>> porFecha = deCancha.stream()
                    .filter(r -> !"CANCELADA".equals(r.getEstado()))
                    .collect(Collectors.groupingBy(Reserva::getFecha));
            Map<Integer, HorarioCancha> semana = new HashMap<>();
            horarios.getOrDefault(cancha.getId_cancha(), List.of()).forEach(h -> semana.put(h.getDiaSemana(), h));
            long disponibles = 0;
            long ocupados = 0;
            int dias = (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
            for (int i = 0; i < dias; i++) {
                LocalDate fecha = desde.plusDays(i);
                HorarioCancha horario = semana.getOrDefault(fecha.getDayOfWeek().getValue(),
                        new HorarioCancha(cancha, fecha.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(23, 0), true));
                List<Intervalo> habilitados = intervalosHabilitados(horario, fecha,
                        bloqueos.getOrDefault(cancha.getId_cancha(), List.of()));
                disponibles += habilitados.stream().mapToLong(Intervalo::segundos).sum() / 60;
                List<Intervalo> reservados = new ArrayList<>();
                for (Reserva reserva : porFecha.getOrDefault(fecha, List.of())) {
                    for (Intervalo habilitado : habilitados) {
                        int inicio = Math.max(habilitado.inicio(), reserva.getHoraInicio().toSecondOfDay());
                        int fin = Math.min(habilitado.fin(), reserva.getHoraFin().toSecondOfDay());
                        if (fin > inicio) reservados.add(new Intervalo(inicio, fin));
                    }
                }
                ocupados += segundosUnidos(reservados) / 60;
            }
            resumen.add(new ReporteDatos.ResumenCancha(cancha.getId_cancha(), cancha.getNombre_cancha(),
                    cancha.getComplejo().getNombre_complejo(), deCancha.size(), deCancha.stream().filter(this::generaIngreso).count(),
                    deCancha.stream().filter(r -> "CANCELADA".equals(r.getEstado())).count(),
                    deCancha.stream().mapToLong(this::ingreso).sum(), disponibles, ocupados));
        }

        Map<String, Long> demanda = new TreeMap<>();
        List<ReporteDatos.DetalleReserva> detalle = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (generaIngreso(reserva)) {
                demanda.merge(String.format("%02d:00", reserva.getHoraInicio().getHour()), 1L, Long::sum);
            }
            detalle.add(new ReporteDatos.DetalleReserva(reserva.getCodigoReserva(), reserva.getFecha(),
                    reserva.getHoraInicio(), reserva.getHoraFin(), reserva.getCancha().getNombre_cancha(),
                    reserva.getCancha().getComplejo().getNombre_complejo(), reserva.getUsuario().getNombre(),
                    reserva.getEstado(), reserva.getPrecioTotal() == null ? 0 : reserva.getPrecioTotal(), ingreso(reserva)));
        }
        return new ReporteDatos(desde, hasta, LocalDateTime.now(), reservas.stream().mapToLong(this::ingreso).sum(),
                reservas.size(), reservas.stream().filter(this::generaIngreso).count(),
                reservas.stream().filter(r -> "CANCELADA".equals(r.getEstado())).count(),
                resumen.stream().mapToLong(ReporteDatos.ResumenCancha::minutosDisponibles).sum(),
                resumen.stream().mapToLong(ReporteDatos.ResumenCancha::minutosOcupados).sum(),
                List.copyOf(resumen), List.copyOf(detalle), Collections.unmodifiableMap(demanda));
    }

    private boolean generaIngreso(Reserva reserva) {
        return "CONFIRMADA".equals(reserva.getEstado()) || "COMPLETADA".equals(reserva.getEstado());
    }

    private int ingreso(Reserva reserva) {
        return generaIngreso(reserva) && reserva.getPrecioTotal() != null ? reserva.getPrecioTotal() : 0;
    }

    private List<Intervalo> intervalosHabilitados(HorarioCancha horario, LocalDate fecha, List<BloqueoHorario> bloqueos) {
        int apertura = horario.getHoraApertura().toSecondOfDay();
        int cierre = horario.getHoraCierre().toSecondOfDay();
        if (!horario.isAbierto() || cierre <= apertura) return List.of();
        List<Intervalo> cierres = new ArrayList<>();
        for (BloqueoHorario bloqueo : bloqueos) {
            if (fecha.isBefore(bloqueo.getFechaInicio()) || fecha.isAfter(bloqueo.getFechaFin())) continue;
            if (bloqueo.isDiaCompleto()) return List.of();
            int inicio = Math.max(apertura, bloqueo.getHoraInicio().toSecondOfDay());
            int fin = Math.min(cierre, bloqueo.getHoraFin().toSecondOfDay());
            if (fin > inicio) cierres.add(new Intervalo(inicio, fin));
        }
        cierres.sort(Comparator.comparingInt(Intervalo::inicio));
        List<Intervalo> habilitados = new ArrayList<>();
        int cursor = apertura;
        for (Intervalo intervalo : cierres) {
            if (intervalo.inicio() > cursor) habilitados.add(new Intervalo(cursor, intervalo.inicio()));
            cursor = Math.max(cursor, intervalo.fin());
        }
        if (cursor < cierre) habilitados.add(new Intervalo(cursor, cierre));
        return habilitados;
    }

    private long segundosUnidos(List<Intervalo> intervalos) {
        intervalos.sort(Comparator.comparingInt(Intervalo::inicio));
        long segundos = 0;
        int inicio = 0;
        int fin = 0;
        for (Intervalo intervalo : intervalos) {
            if (intervalo.inicio() > fin) {
                segundos += fin - inicio;
                inicio = intervalo.inicio();
                fin = intervalo.fin();
            } else {
                fin = Math.max(fin, intervalo.fin());
            }
        }
        return segundos + fin - inicio;
    }

    private record Intervalo(int inicio, int fin) {
        long segundos() { return fin - inicio; }
    }
}
