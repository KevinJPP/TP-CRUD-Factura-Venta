package com.tp.facturacionapi.arca;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class ArcaWsaaService {

    private static final long DURACION_CACHE_MS = 10L * 60L * 60L * 1000L;

    @Value("${arca.wsaa.url}")
    private String wsaaUrl;

    @Value("${arca.private-key-path}")
    private String privateKeyPath;

    @Value("${arca.cert-path}")
    private String certPath;

    @Value("${arca.cache-path:ta-wsfe.cache}")
    private String cachePathStr;

    public record TokenSign(String token, String sign) {}

    public synchronized TokenSign obtenerTokenYSign() throws Exception {
        Path cachePath = Path.of(cachePathStr);
        if (Files.exists(cachePath)) {
            long edadMs = System.currentTimeMillis() - Files.getLastModifiedTime(cachePath).toMillis();
            if (edadMs >= 0 && edadMs < DURACION_CACHE_MS) {
                List<String> lineas = Files.readAllLines(cachePath, StandardCharsets.UTF_8);
                if (lineas.size() >= 2 && !lineas.get(0).isBlank() && !lineas.get(1).isBlank()) {
                    return new TokenSign(lineas.get(0).trim(), lineas.get(1).trim());
                }
            }
        }

        TokenSign nuevo = solicitarNuevoTokenYSign();
        Files.writeString(cachePath, nuevo.token() + "\n" + nuevo.sign() + "\n", StandardCharsets.UTF_8);
        return nuevo;
    }

    private TokenSign solicitarNuevoTokenYSign() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        PrivateKey privateKey = cargarClavePrivadaPkcs8(privateKeyPath);
        X509Certificate cert = cargarCertificado(certPath);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        String genTime = now.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expTime = now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String traXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                + "<header>"
                + "<uniqueId>" + (System.currentTimeMillis() / 1000) + "</uniqueId>"
                + "<generationTime>" + genTime + "</generationTime>"
                + "<expirationTime>" + expTime + "</expirationTime>"
                + "</header>"
                + "<service>wsfe</service>"
                + "</loginTicketRequest>";

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);

        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                ).build(sha256Signer, cert)
        );
        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)),
                true
        );

        String cmsBase64 = Base64.getEncoder().encodeToString(signedData.getEncoded());

        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:nso=\"http://wsaa.afip.gov.ar/ws/services/LoginCms\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<nso:loginCms>"
                + "<nso:in0>" + cmsBase64 + "</nso:in0>"
                + "</nso:loginCms>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        String xmlResponse = enviarSoap(wsaaUrl, soapEnvelope);
        String xmlLimpio = xmlResponse.replace("&lt;", "<").replace("&gt;", ">");

        String token = extraerTag(xmlLimpio, "token");
        String sign = extraerTag(xmlLimpio, "sign");
        return new TokenSign(token, sign);
    }

    private String extraerTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close);
        if (start == -1 || end == -1) {
            throw new IllegalStateException("No se encontro <" + tag + "> en la respuesta WSAA: " + xml);
        }
        return xml.substring(start + open.length(), end);
    }

    private PrivateKey cargarClavePrivadaPkcs8(String path) throws Exception {
        String content = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        content = content
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(content);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private X509Certificate cargarCertificado(String path) throws Exception {
        try (InputStream is = new FileInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
    }

    private String enviarSoap(String urlStr, String soapXml) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");
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
                throw new IOException("HTTP " + status + " desde WSAA: " + sb);
            }
            return sb.toString();
        }
    }
}