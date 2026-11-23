package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

public class FidelizacionInfo {
    private int reservasCompletadas;
    private int meta = 2;
    private int faltantes;
    private boolean descuentoDisponible;
    private int porcentajeDescuento = 20;

    public FidelizacionInfo(int reservasCompletadas) {
        this.reservasCompletadas = Math.min(reservasCompletadas, 2);
        this.faltantes = Math.max(0, 2 - this.reservasCompletadas);
        this.descuentoDisponible = this.reservasCompletadas >= 2;
    }

    public int getReservasCompletadas() {
        return reservasCompletadas;
    }

    public int getMeta() {
        return meta;
    }

    public int getFaltantes() {
        return faltantes;
    }

    public boolean isDescuentoDisponible() {
        return descuentoDisponible;
    }

    public int getPorcentajeDescuento() {
        return porcentajeDescuento;
    }
}
