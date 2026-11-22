package cl.jimenez.munoz.aguilera.baeza.proyectosemestral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProyectosemestralApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProyectosemestralApplication.class, args);
    }

}
