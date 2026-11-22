package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ReporteDatos;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteExportacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale CHILE = Locale.forLanguageTag("es-CL");
    private static final String CRITERIOS = "Fechas según el día del partido. Ingresos: reservas confirmadas o completadas. "
            + "Ocupación: tiempo de reservas no canceladas sobre horas habilitadas, descontando bloqueos. "
            + "La capacidad usa los horarios semanales y bloqueos registrados actualmente.";

    public byte[] generarExcel(ReporteDatos datos) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(libro);
            Sheet resumen = libro.createSheet("Resumen");
            cabeceraHoja(resumen, datos, estilos, "Resumen del período", 4);
            String[] etiquetas = {"Ingresos (CLP)", "Reservas totales", "Confirmadas o completadas", "Canceladas",
                    "Horas habilitadas", "Horas ocupadas", "Ocupación"};
            double[] valores = {datos.totalIngresos(), datos.totalReservas(), datos.confirmadas(), datos.canceladas(),
                    datos.minutosDisponibles() / 60.0, datos.minutosOcupados() / 60.0, datos.tasaOcupacion() / 100.0};
            for (int i = 0; i < etiquetas.length; i++) {
                Row fila = resumen.createRow(i + 4);
                escribir(fila, 0, etiquetas[i], estilos.texto);
                escribir(fila, 1, valores[i], i == 0 ? estilos.moneda : i == 6 ? estilos.porcentaje : i >= 4 ? estilos.decimal : estilos.entero);
            }
            Row criterios = resumen.createRow(13);
            escribir(criterios, 0, CRITERIOS, estilos.texto);
            criterios.setHeightInPoints(65);
            resumen.addMergedRegion(new CellRangeAddress(13, 13, 0, 3));
            resumen.setColumnWidth(0, 38 * 256);
            resumen.setColumnWidth(1, 24 * 256);
            resumen.setColumnWidth(2, 22 * 256);
            resumen.setColumnWidth(3, 22 * 256);

            crearHojaReservas(libro, datos, estilos, false);
            crearHojaReservas(libro, datos, estilos, true);
            Sheet ocupacion = libro.createSheet("Ocupación");
            cabeceraHoja(ocupacion, datos, estilos, "Ocupación e ingresos por cancha", 10);
            encabezados(ocupacion, estilos, "ID cancha", "Complejo", "Cancha", "Reservas", "Confirmadas", "Canceladas",
                    "Horas habilitadas", "Horas ocupadas", "Ocupación", "Ingresos (CLP)");
            int fila = 4;
            for (ReporteDatos.ResumenCancha cancha : datos.canchas()) {
                Row row = ocupacion.createRow(fila++);
                escribir(row, 0, cancha.id(), estilos.entero);
                escribir(row, 1, cancha.complejo(), estilos.texto);
                escribir(row, 2, cancha.cancha(), estilos.texto);
                escribir(row, 3, cancha.reservas(), estilos.entero);
                escribir(row, 4, cancha.confirmadas(), estilos.entero);
                escribir(row, 5, cancha.canceladas(), estilos.entero);
                escribir(row, 6, cancha.minutosDisponibles() / 60.0, estilos.decimal);
                escribir(row, 7, cancha.minutosOcupados() / 60.0, estilos.decimal);
                escribir(row, 8, cancha.tasaOcupacion() / 100.0, estilos.porcentaje);
                escribir(row, 9, cancha.ingresos(), estilos.moneda);
                ajustarAltura(row);
            }
            finalizarTabla(ocupacion, 10);
            libro.setActiveSheet(0);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private void crearHojaReservas(XSSFWorkbook libro, ReporteDatos datos, Estilos estilos, boolean soloIngresos) {
        Sheet hoja = libro.createSheet(soloIngresos ? "Ingresos" : "Reservas");
        cabeceraHoja(hoja, datos, estilos, soloIngresos ? "Ingresos confirmados por reserva" : "Detalle de todas las reservas", 9);
        encabezados(hoja, estilos, "Código", "Fecha del partido", "Inicio", "Fin", "Complejo", "Cancha", "Cliente", "Estado",
                soloIngresos ? "Ingreso (CLP)" : "Precio total (CLP)");
        int fila = 4;
        for (ReporteDatos.DetalleReserva reserva : datos.reservas()) {
            if (soloIngresos && !"CONFIRMADA".equals(reserva.estado()) && !"COMPLETADA".equals(reserva.estado())) continue;
            Row row = hoja.createRow(fila++);
            escribir(row, 0, reserva.codigo(), estilos.texto);
            escribir(row, 1, reserva.fecha(), estilos.fecha);
            escribir(row, 2, reserva.inicio(), estilos.texto);
            escribir(row, 3, reserva.fin(), estilos.texto);
            escribir(row, 4, reserva.complejo(), estilos.texto);
            escribir(row, 5, reserva.cancha(), estilos.texto);
            escribir(row, 6, reserva.cliente(), estilos.texto);
            escribir(row, 7, reserva.estado(), estilos.texto);
            escribir(row, 8, soloIngresos ? reserva.ingresoReconocido() : reserva.precioTotal(), estilos.moneda);
            ajustarAltura(row);
        }
        finalizarTabla(hoja, 9);
        hoja.setColumnWidth(2, 11 * 256);
        hoja.setColumnWidth(3, 11 * 256);
    }

    private void cabeceraHoja(Sheet hoja, ReporteDatos datos, Estilos estilos, String titulo, int columnas) {
        Row fila = hoja.createRow(0);
        escribir(fila, 0, "CanchasYa · " + titulo, estilos.titulo);
        fila.setHeightInPoints(30);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas - 1));
        escribir(hoja.createRow(1), 0, "Período: " + FECHA.format(datos.desde()) + " al " + FECHA.format(datos.hasta()), estilos.texto);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, columnas - 1));
        escribir(hoja.createRow(2), 0, "Generado: " + datos.generadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), estilos.texto);
        hoja.addMergedRegion(new CellRangeAddress(2, 2, 0, columnas - 1));
        hoja.setFitToPage(true);
        hoja.getPrintSetup().setLandscape(true);
        hoja.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        hoja.getPrintSetup().setFitWidth((short) 1);
        hoja.getPrintSetup().setFitHeight((short) 0);
        hoja.getFooter().setRight("Página &P de &N");
    }

    private void encabezados(Sheet hoja, Estilos estilos, String... columnas) {
        Row fila = hoja.createRow(3);
        fila.setHeightInPoints(32);
        for (int i = 0; i < columnas.length; i++) escribir(fila, i, columnas[i], estilos.encabezado);
    }

    private void finalizarTabla(Sheet hoja, int columnas) {
        hoja.createFreezePane(0, 4);
        hoja.setAutoFilter(new CellRangeAddress(3, Math.max(3, hoja.getLastRowNum()), 0, columnas - 1));
        hoja.setRepeatingRows(new CellRangeAddress(0, 3, -1, -1));
        for (int i = 0; i < columnas; i++) hoja.setColumnWidth(i, 23 * 256);
    }

    private void escribir(Row fila, int columna, Object valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellStyle(estilo);
        if (valor instanceof Number numero) celda.setCellValue(numero.doubleValue());
        else if (valor instanceof LocalDate fecha) celda.setCellValue(fecha.atStartOfDay());
        else if (valor instanceof LocalTime hora) celda.setCellValue(HORA.format(hora));
        // Los textos del usuario siempre son celdas de texto, incluso si empiezan por =, +, - o @.
        else celda.setCellValue(valor == null ? "" : valor.toString());
    }

    private void ajustarAltura(Row fila) {
        int lineas = 1;
        for (Cell celda : fila) {
            if (celda.getCellType() == CellType.STRING) {
                lineas = Math.max(lineas, (celda.getStringCellValue().length() + 19) / 20);
            }
        }
        fila.setHeightInPoints(Math.max(32, Math.min(300, lineas * 14 + 8)));
    }

    private static class Estilos {
        final CellStyle titulo, encabezado, texto, entero, moneda, porcentaje, decimal, fecha;

        Estilos(XSSFWorkbook libro) {
            texto = libro.createCellStyle();
            texto.setVerticalAlignment(VerticalAlignment.CENTER);
            texto.setWrapText(true);
            org.apache.poi.ss.usermodel.Font fuente = libro.createFont();
            fuente.setBold(true);
            fuente.setFontHeightInPoints((short) 16);
            titulo = libro.createCellStyle(); titulo.setFont(fuente);
            org.apache.poi.ss.usermodel.Font blanca = libro.createFont();
            blanca.setBold(true); blanca.setColor(IndexedColors.WHITE.getIndex());
            encabezado = libro.createCellStyle(); encabezado.cloneStyleFrom(texto); encabezado.setFont(blanca);
            ((XSSFCellStyle) encabezado).setFillForegroundColor(
                    new XSSFColor(new Color(51, 71, 58), new DefaultIndexedColorMap()));
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            entero = formato(libro, "#,##0");
            moneda = formato(libro, "#,##0 \"CLP\"");
            porcentaje = formato(libro, "0.0%");
            decimal = formato(libro, "0.0");
            fecha = formato(libro, "dd/mm/yyyy");
        }

        private CellStyle formato(XSSFWorkbook libro, String formato) {
            CellStyle estilo = libro.createCellStyle(); estilo.cloneStyleFrom(texto);
            estilo.setDataFormat(libro.createDataFormat().getFormat(formato));
            return estilo;
        }
    }

    public byte[] generarPdf(ReporteDatos datos) throws IOException {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            documento.getDocumentInformation().setTitle("CanchasYa - Reporte de ingresos, reservas y ocupación");
            try (TablaPdf pdf = new TablaPdf(documento, datos)) {
                pdf.seccion("Resumen del período");
                pdf.parrafo("Ingresos: " + dinero(datos.totalIngresos()) + "   |   Reservas: " + datos.totalReservas()
                        + "   |   Confirmadas/completadas: " + datos.confirmadas() + "   |   Canceladas: " + datos.canceladas());
                pdf.parrafo("Ocupación: " + decimal(datos.tasaOcupacion()) + "%   |   Horas ocupadas: "
                        + decimal(datos.minutosOcupados() / 60.0) + "   |   Horas habilitadas: " + decimal(datos.minutosDisponibles() / 60.0));
                pdf.parrafo(CRITERIOS);

                pdf.seccion("Ingresos por cancha");
                List<List<String>> ingresos = new ArrayList<>();
                for (ReporteDatos.ResumenCancha c : datos.canchas()) {
                    ingresos.add(List.of(c.cancha(), c.complejo(), String.valueOf(c.confirmadas()), dinero(c.ingresos())));
                }
                pdf.tabla(new String[]{"Cancha", "Complejo", "Confirmadas/completadas", "Ingresos (CLP)"},
                        new float[]{220, 250, 170, 130}, ingresos);

                pdf.seccion("Ocupación por cancha");
                List<List<String>> ocupacion = new ArrayList<>();
                for (ReporteDatos.ResumenCancha c : datos.canchas()) {
                    ocupacion.add(List.of(c.cancha(), c.complejo(), decimal(c.minutosDisponibles() / 60.0),
                            decimal(c.minutosOcupados() / 60.0), decimal(c.tasaOcupacion()) + "%"));
                }
                pdf.tabla(new String[]{"Cancha", "Complejo", "Horas habilitadas", "Horas ocupadas", "Ocupación"},
                        new float[]{200, 240, 120, 120, 90}, ocupacion);

                pdf.seccion("Detalle de reservas");
                List<List<String>> reservas = new ArrayList<>();
                for (ReporteDatos.DetalleReserva r : datos.reservas()) {
                    reservas.add(List.of(r.codigo(), FECHA.format(r.fecha()), HORA.format(r.inicio()) + " - " + HORA.format(r.fin()),
                            r.cancha() + " / " + r.complejo(), r.cliente(), r.estado(), dinero(r.precioTotal())));
                }
                pdf.tabla(new String[]{"Código", "Fecha", "Horario", "Cancha / complejo", "Cliente", "Estado", "Total (CLP)"},
                        new float[]{88, 72, 88, 190, 135, 92, 105}, reservas);
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private static String dinero(long monto) { return "$" + String.format(CHILE, "%,d", monto); }
    private static String decimal(double valor) { return String.format(CHILE, "%.1f", valor); }

    private static class TablaPdf implements AutoCloseable {
        private static final Color VERDE = new Color(51, 71, 58);
        private final PDDocument documento;
        private final ReporteDatos datos;
        private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream contenido;
        private float y;

        TablaPdf(PDDocument documento, ReporteDatos datos) throws IOException {
            this.documento = documento; this.datos = datos; nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            cerrarPagina();
            PDPage pagina = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            y = pagina.getMediaBox().getHeight() - 38;
            texto("CanchasYa | Reporte de gestión", 36, y, negrita, 18, VERDE);
            y -= 20;
            texto("Período: " + FECHA.format(datos.desde()) + " al " + FECHA.format(datos.hasta()), 36, y, normal, 10, Color.DARK_GRAY);
            y -= 15;
            texto("Generado: " + datos.generadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), 36, y, normal, 8, Color.GRAY);
            y -= 24;
        }

        private void asegurar(float alto) throws IOException { if (y - alto < 42) nuevaPagina(); }

        void seccion(String titulo) throws IOException {
            asegurar(60); y -= 6;
            texto(titulo, 36, y, negrita, 12, VERDE); y -= 19;
        }

        void parrafo(String valor) throws IOException {
            for (String linea : lineas(valor, normal, 9, 770)) {
                asegurar(13); texto(linea, 36, y, normal, 9, Color.DARK_GRAY); y -= 13;
            }
            y -= 6;
        }

        void tabla(String[] encabezados, float[] anchos, List<List<String>> filas) throws IOException {
            asegurar(65);
            fila(List.of(encabezados), anchos, true, false);
            if (filas.isEmpty()) { parrafo("Sin registros en este período."); return; }
            int indice = 0;
            for (List<String> valores : filas) {
                float alto = altoFila(valores, anchos, normal);
                if (y - alto < 42) {
                    nuevaPagina(); fila(List.of(encabezados), anchos, true, false);
                }
                fila(valores, anchos, false, indice++ % 2 == 0);
            }
            y -= 12;
        }

        private float altoFila(List<String> valores, float[] anchos, PDFont fuente) throws IOException {
            int maximo = 1;
            for (int i = 0; i < anchos.length; i++) maximo = Math.max(maximo, lineas(valores.get(i), fuente, 8, anchos[i] - 12).size());
            return maximo * 11 + 12;
        }

        private void fila(List<String> valores, float[] anchos, boolean encabezado, boolean alterna) throws IOException {
            PDFont fuente = encabezado ? negrita : normal;
            float alto = altoFila(valores, anchos, fuente);
            contenido.setNonStrokingColor(encabezado ? VERDE : alterna ? new Color(245, 246, 242) : Color.WHITE);
            contenido.addRect(36, y - alto, 770, alto); contenido.fill();
            float x = 36;
            for (int i = 0; i < anchos.length; i++) {
                float lineaY = y - 14;
                for (String linea : lineas(valores.get(i), fuente, 8, anchos[i] - 12)) {
                    texto(linea, x + 6, lineaY, fuente, 8, encabezado ? Color.WHITE : Color.DARK_GRAY);
                    lineaY -= 11;
                }
                x += anchos[i];
            }
            y -= alto;
        }

        private List<String> lineas(String valor, PDFont fuente, float tamano, float ancho) throws IOException {
            StringBuilder limpio = new StringBuilder();
            for (int cp : (valor == null ? "" : valor).codePoints().toArray()) {
                String caracter = Character.isISOControl(cp) ? " " : new String(Character.toChars(cp));
                try { fuente.encode(caracter); limpio.append(caracter); }
                catch (IllegalArgumentException e) { limpio.append('?'); }
            }
            List<String> lineas = new ArrayList<>();
            StringBuilder linea = new StringBuilder();
            for (String palabra : limpio.toString().split("\\s+")) {
                String intento = linea.isEmpty() ? palabra : linea + " " + palabra;
                if (fuente.getStringWidth(intento) * tamano / 1000 <= ancho) {
                    linea.setLength(0); linea.append(intento);
                } else {
                    if (!linea.isEmpty()) { lineas.add(linea.toString()); linea.setLength(0); }
                    for (char caracter : palabra.toCharArray()) {
                        if (!linea.isEmpty() && fuente.getStringWidth(linea.toString() + caracter) * tamano / 1000 > ancho) {
                            lineas.add(linea.toString()); linea.setLength(0);
                        }
                        linea.append(caracter);
                    }
                }
            }
            if (!linea.isEmpty() || lineas.isEmpty()) lineas.add(linea.toString());
            return lineas;
        }

        private void texto(String texto, float x, float y, PDFont fuente, float tamano, Color color) throws IOException {
            contenido.beginText(); contenido.setFont(fuente, tamano); contenido.setNonStrokingColor(color);
            contenido.newLineAtOffset(x, y); contenido.showText(texto); contenido.endText();
        }

        private void cerrarPagina() throws IOException {
            if (contenido != null) {
                texto("CanchasYa · Página " + documento.getNumberOfPages(), 36, 22, normal, 8, Color.GRAY);
                contenido.close(); contenido = null;
            }
        }

        @Override public void close() throws IOException { cerrarPagina(); }
    }
}
