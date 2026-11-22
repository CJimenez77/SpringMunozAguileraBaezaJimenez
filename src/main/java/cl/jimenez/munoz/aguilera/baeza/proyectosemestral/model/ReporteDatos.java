package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record ReporteDatos(LocalDate desde, LocalDate hasta, LocalDateTime generadoEn,
                           long totalIngresos, long totalReservas, long confirmadas, long canceladas,
                           long minutosDisponibles, long minutosOcupados,
                           List<ResumenCancha> canchas, List<DetalleReserva> reservas,
                           Map<String, Long> horariosDemandados) {

    public double tasaOcupacion() {
        return porcentaje(minutosOcupados, minutosDisponibles);
    }

    public static double porcentaje(long ocupados, long disponibles) {
        return disponibles > 0 ? Math.round(ocupados * 1000.0 / disponibles) / 10.0 : 0;
    }

    public record ResumenCancha(Long id, String cancha, String complejo, long reservas, long confirmadas,
                                long canceladas, long ingresos, long minutosDisponibles, long minutosOcupados) {
        public double tasaOcupacion() { return porcentaje(minutosOcupados, minutosDisponibles); }
    }

    public record DetalleReserva(String codigo, LocalDate fecha, LocalTime inicio, LocalTime fin,
                                 String cancha, String complejo, String cliente, String estado,
                                 int precioTotal, int ingresoReconocido) {}
}
