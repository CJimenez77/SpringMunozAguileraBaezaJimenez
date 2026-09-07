package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReporteService {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;

    public ReporteService(ReservaRepository reservaRepository, CanchaRepository canchaRepository) {
        this.reservaRepository = reservaRepository;
        this.canchaRepository = canchaRepository;
    }

    public Map<String, Object> generarReportes(Usuario usuario) {
        List<Reserva> reservas;
        List<Cancha> canchas;

        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            canchas = canchaRepository.findByDuenoAndActivoTrue(usuario);
            reservas = canchas.isEmpty() ? Collections.emptyList()
                    : reservaRepository.findByCanchaInOrderByFechaDescHoraInicioDesc(canchas);
        } else {
            canchas = canchaRepository.findByActivoTrue();
            reservas = reservaRepository.findAll();
        }

        long totalIngresos = 0;
        long totalReservas = reservas.size();
        long confirmadas = 0;
        long canceladas = 0;

        Map<String, Long> ingresosPorCancha = new LinkedHashMap<>();
        Map<String, Long> reservasPorCancha = new LinkedHashMap<>();
        Map<String, Long> horariosDemandados = new TreeMap<>();

        for (Cancha c : canchas) {
            ingresosPorCancha.put(c.getNombre_cancha(), 0L);
            reservasPorCancha.put(c.getNombre_cancha(), 0L);
        }

        for (Reserva r : reservas) {
            String nombreCancha = r.getCancha().getNombre_cancha();
            if ("CONFIRMADA".equals(r.getEstado()) || "COMPLETADA".equals(r.getEstado())) {
                confirmadas++;
                long monto = r.getPrecioTotal() != null ? r.getPrecioTotal() : 0;
                totalIngresos += monto;

                ingresosPorCancha.put(nombreCancha, ingresosPorCancha.getOrDefault(nombreCancha, 0L) + monto);
                reservasPorCancha.put(nombreCancha, reservasPorCancha.getOrDefault(nombreCancha, 0L) + 1);

                String horaTexto = String.format("%02d:00", r.getHoraInicio().getHour());
                horariosDemandados.put(horaTexto, horariosDemandados.getOrDefault(horaTexto, 0L) + 1);
            } else if ("CANCELADA".equals(r.getEstado())) {
                canceladas++;
            }
        }

        double tasaOcupacion = totalReservas > 0 ? ((double) confirmadas / totalReservas) * 100 : 0.0;

        Map<String, Object> reporte = new HashMap<>();
        reporte.put("totalIngresos", totalIngresos);
        reporte.put("totalReservas", totalReservas);
        reporte.put("confirmadas", confirmadas);
        reporte.put("canceladas", canceladas);
        reporte.put("tasaOcupacion", Math.round(tasaOcupacion * 10.0) / 10.0);
        reporte.put("ingresosPorCancha", ingresosPorCancha);
        reporte.put("reservasPorCancha", reservasPorCancha);
        reporte.put("horariosDemandados", horariosDemandados);

        return reporte;
    }
}
