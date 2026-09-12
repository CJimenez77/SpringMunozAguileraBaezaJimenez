package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.config;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ServicioAdicional;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ServicioAdicionalRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Puebla la base de datos con datos de prueba la primera vez que se levanta la app.
 * Si ya existen usuarios (por ejemplo, en un reinicio), no hace nada, para no duplicar
 * ni chocar con la restricción de correo único.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ComplejoRepositorio complejoRepositorio;
    private final CanchaRepository canchaRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      ComplejoRepositorio complejoRepositorio,
                      CanchaRepository canchaRepository,
                      ServicioAdicionalRepository servicioAdicionalRepository) {
        this.usuarioRepository = usuarioRepository;
        this.complejoRepositorio = complejoRepositorio;
        this.canchaRepository = canchaRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            System.out.println("[DataSeeder] Ya existen datos en la base, no se vuelve a poblar.");
            return;
        }

        // 1. Los 3 usuarios base, uno por cada rol del sistema
        Usuario adminSistema = new Usuario(
                "Admin General", "admin@canchasya.cl", "900000001",
                "admin123", "ADMIN_SISTEMA", null);

        Usuario adminComplejo = new Usuario(
                "Dueño de Complejo", "complejo@canchasya.cl", "900000002",
                "complejo123", "ADMIN_COMPLEJO", null);

        Usuario cliente = new Usuario(
                "Cliente Demo", "cliente@canchasya.cl", "900000003",
                "cliente123", "CLIENTE", null);

        usuarioRepository.save(adminSistema);
        usuarioRepository.save(adminComplejo);
        usuarioRepository.save(cliente);

        // 2. Dos complejos, ambos administrados por el ADMIN_COMPLEJO de prueba
        Complejo complejo1 = new Complejo(
                "Centro Deportivo Las Condes", "Av. Apoquindo 1234, Las Condes",
                "https://maps.google.com/?q=Las+Condes", adminComplejo);

        Complejo complejo2 = new Complejo(
                "Polideportivo Ñuñoa", "Av. Irarrázaval 4321, Ñuñoa",
                "https://maps.google.com/?q=Nunoa", adminComplejo);

        complejoRepositorio.save(complejo1);
        complejoRepositorio.save(complejo2);

        // 3. Un par de canchas por complejo, con distintos deportes (foto queda null a propósito:
        //    las plantillas ya muestran un placeholder automático cuando no hay foto)
        Cancha cancha1 = new Cancha("Cancha Fútbol 1", "Fútbol", 15000, complejo1);
        Cancha cancha2 = new Cancha("Cancha Pádel A", "Pádel", 12000, complejo1);
        Cancha cancha3 = new Cancha("Cancha Tenis 1", "Tenis", 10000, complejo2);
        Cancha cancha4 = new Cancha("Cancha Básquetbol", "Básquetbol", 8000, complejo2);

        canchaRepository.save(cancha1);
        canchaRepository.save(cancha2);
        canchaRepository.save(cancha3);
        canchaRepository.save(cancha4);

        // 4. Servicios adicionales de ejemplo, para mostrar esa funcionalidad también
        servicioAdicionalRepository.save(new ServicioAdicional(
                complejo1, "Iluminación", "Luces para partidos nocturnos", 3000));
        servicioAdicionalRepository.save(new ServicioAdicional(
                complejo1, "Camarines", "Acceso a camarines con casilleros", 2000));
        servicioAdicionalRepository.save(new ServicioAdicional(
                complejo2, "Arriendo de pelotas", "Set de pelotas para el partido", 1500));

        System.out.println("[DataSeeder] Datos de prueba creados: 3 usuarios, 2 complejos, 4 canchas, 3 servicios.");
    }
}