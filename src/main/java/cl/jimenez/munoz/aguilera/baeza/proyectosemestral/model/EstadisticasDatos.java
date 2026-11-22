package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record EstadisticasDatos(LocalDate desde, LocalDate hasta, long reservasAnalizadas,
                                 long canchasReservadas, long clientesDistintos,
                                 Double calificacionPromedio, long totalOpiniones,
                                 List<CanchaRanking> canchas, List<HoraRanking> horas,
                                 List<CalificacionCancha> calificaciones, List<ClienteRanking> clientes,
                                 Map<String, Long> demandaPorHora) {

    public record CanchaRanking(Long id, String nombre, String complejo, long reservas) {}
    public record HoraRanking(String hora, long reservas) {}
    public record CalificacionCancha(Long id, String nombre, String complejo, Double promedio, long opiniones) {}
    public record ClienteRanking(Long id, String nombre, String email, long reservas) {}
}
