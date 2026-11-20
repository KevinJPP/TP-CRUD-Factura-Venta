package com.tp.facturacionapi.arca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class ArcaWsfeService {

    private static final String NS_WSFE = "http://ar.gov.afip.dif.FEV1/";

    private final ArcaWsaaService wsaaService;

    @Value("${arca.wsfe.url}")
    private String wsfeUrl;

    @Value("${arca.cuit}")
    private long cuit;

    @Value("${arca.pto-vta}")
    private int ptoVta;

    @Value("${arca.cbte-tipo}")
    private int cbteTipo;

    @Value("${arca.condicion-iva-receptor-id:5}")
    private int condicionIvaReceptorId;

    public ArcaWsfeService(ArcaWsaaService wsaaService) {
        this.wsaaService = wsaaService;
    }

    public record CaeResultado(
            long numeroComprobante,
            String resultado,
            String cae,
            LocalDateTime caeFechaVencimiento,
            String motivoRechazo
    ) {}

    public CaeResultado autorizarComprobante(LocalDate fechaEmision, double importeTotal) throws Exception {
        ArcaWsaaService.TokenSign auth = wsaaService.obtenerTokenYSign();

        long ultimoNro = consultarUltimoComprobante(auth.token(), auth.sign());
        long nuevoNro = ultimoNro + 1;
        String fechaYyyyMmDd = fechaEmision.format(DateTimeFormatter.BASIC_ISO_DATE);

        String respuestaXml = solicitarCae(auth.token(), auth.sign(), nuevoNro, fechaYyyyMmDd, importeTotal);

        String resultado = extraerTagSiExiste(respuestaXml, "Resultado");
        String cae = extraerTagSiExiste(respuestaXml, "CAE");
        String vtoStr = extraerTagSiExiste(respuestaXml, "CAEFchVto");
        String msg = extraerTagSiExiste(respuestaXml, "Msg");

        LocalDateTime fechaVto = null;
        if (vtoStr != null && !vtoStr.isBlank()) {
            LocalDate ld = LocalDate.parse(vtoStr.trim(), DateTimeFormatter.BASIC_ISO_DATE);
            fechaVto = ld.atTime(LocalTime.MAX);
        }

        String caeLimpio = (cae != null && !cae.isBlank()) ? cae.trim() : null;
        String motivo = ("A".equalsIgnoreCase(resultado) && caeLimpio != null)
                ? null
                : (msg != null ? msg.trim() : respuestaXml);

        return new CaeResultado(nuevoNro, resultado, caeLimpio, fechaVto, motivo);
    }

    private long consultarUltimoComprobante(String token, String sign) throws Exception {
        String soapXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + "<FECompUltimoAutorizado xmlns=\"" + NS_WSFE + "\">"
                + "<Auth>"
                + "<Token>" + escapeXml(token) + "</Token>"
                + "<Sign>" + escapeXml(sign) + "</Sign>"
                + "<Cuit>" + cuit + "</Cuit>"
                + "</Auth>"
                + "<PtoVta>" + ptoVta + "</PtoVta>"
                + "<CbteTipo>" + cbteTipo + "</CbteTipo>"
                + "</FECompUltimoAutorizado>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        String resp = enviarSoap(wsfeUrl, NS_WSFE + "FECompUltimoAutorizado", soapXml);
        String nroStr = extraerTagSiExiste(resp, "CbteNro");
        if (nroStr == null) {
            throw new IllegalStateException("No se encontro CbteNro en la respuesta: " + resp);
        }
        return Long.parseLong(nroStr.trim());
    }

    private String solicitarCae(String token, String sign, long nroComprobante,
                                String fechaYyyyMmDd, double importeTotal) throws Exception {
        String soapXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + "<FECAESolicitar xmlns=\"" + NS_WSFE + "\">"
                + "<Auth>"
                + "<Token>" + escapeXml(token) + "</Token>"
                + "<Sign>" + escapeXml(sign) + "</Sign>"
                + "<Cuit>" + cuit + "</Cuit>"
                + "</Auth>"
                + "<FeCAEReq>"
                + "<FeCabReq>"
                + "<CantReg>1</CantReg>"
                + "<PtoVta>" + ptoVta + "</PtoVta>"
                + "<CbteTipo>" + cbteTipo + "</CbteTipo>"
                + "</FeCabReq>"
                + "<FeDetReq>"
                + "<FECAEDetRequest>"
                + "<Concepto>1</Concepto>"
                + "<DocTipo>99</DocTipo>"
                + "<DocNro>0</DocNro>"
                + "<CbteDesde>" + nroComprobante + "</CbteDesde>"
                + "<CbteHasta>" + nroComprobante + "</CbteHasta>"
                + "<CbteFch>" + fechaYyyyMmDd + "</CbteFch>"
                + "<ImpTotal>" + importeTotal + "</ImpTotal>"
                + "<ImpTotConc>0.0</ImpTotConc>"
                + "<ImpNeto>" + importeTotal + "</ImpNeto>"
                + "<ImpOpEx>0.0</ImpOpEx>"
                + "<ImpTrib>0.0</ImpTrib>"
                + "<ImpIVA>0.0</ImpIVA>"
                + "<MonId>PES</MonId>"
                + "<MonCotiz>1.0</MonCotiz>"
                + "<CondicionIVAReceptorId>" + condicionIvaReceptorId + "</CondicionIVAReceptorId>"
                + "</FECAEDetRequest>"
                + "</FeDetReq>"
                + "</FeCAEReq>"
                + "</FECAESolicitar>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        return enviarSoap(wsfeUrl, NS_WSFE + "FECAESolicitar", soapXml);
    }

    private String extraerTagSiExiste(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close);
        if (start == -1 || end == -1) {
            return null;
        }
        return xml.substring(start + open.length(), end);
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String enviarSoap(String urlStr, String soapAction, String soapXml) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "\"" + soapAction + "\"");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (status >= 400) {
                throw new IOException("HTTP " + status + " desde WSFE: " + sb);
            }
            return sb.toString();
        }
    }
}