// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: ArasaacService.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que conecta con la API pública de ARASAAC para obtener pictogramas.
 *
 * Se usa en los módulos de chat y emociones. Guarda en caché los resultados
 * para no volver a pedir lo mismo a la API y así ir más rápido.
 *
 * Las peticiones HTTP usan {@link HttpClient} (Java 11+) con un tiempo
 * máximo de espera de 8 segundos. Si la API no responde, los métodos
 * devuelven listas vacías o emojis como alternativa.
 *
 * @see <a href="https://api.arasaac.org">Documentación de la API de ARASAAC</a>
 */
@Service
public class ArasaacService {

    /** URL base de la API de búsqueda. */
    private static final String API_URL = "https://api.arasaac.org/v1/pictograms";

    /** URL base para construir las URLs de las imágenes PNG. */
    private static final String IMG_URL = "https://static.arasaac.org/pictograms/";

    /** Cliente HTTP reutilizable con 8 segundos de timeout. */
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /** Para parsear el JSON que devuelve la API. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Caché en memoria con los resultados de búsquedas anteriores.
     * La clave es la palabra buscada; el valor, la lista de pictogramas.
     * Usamos {@link ConcurrentHashMap} porque varios hilos pueden acceder a la vez.
     */
    private final ConcurrentHashMap<String, List<PictogramaDTO>> cache =
            new ConcurrentHashMap<>();

    /**
     * Devuelve la URL de la imagen PNG de un pictograma a partir de su identificador.
     *
     * @param id el identificador del pictograma en ARASAAC
     * @return URL completa de la imagen en 500px
     */
    public String getImagenUrl(int id) {
        return IMG_URL + id + "/" + id + "_500.png";
    }

    /**
     * Busca pictogramas en ARASAAC por palabra clave en español.
     *
     * Si ya buscamos esa palabra antes, devuelve el resultado de la caché
     * sin hacer ninguna petición HTTP. Devuelve como máximo 12 resultados.
     *
     * @param palabra el término de búsqueda
     * @return lista de hasta 12 pictogramas; lista vacía si la API no responde o no hay resultados
     */
    public List<PictogramaDTO> buscar(String palabra) {
        String clave = palabra.toLowerCase().trim();
        if (cache.containsKey(clave)) return cache.get(clave);

        List<PictogramaDTO> resultado = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(clave, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/es/search/" + encoded))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode array = mapper.readTree(response.body());
            int max = Math.min(array.size(), 12);
            for (int i = 0; i < max; i++) {
                JsonNode node = array.get(i);
                int id = node.get("_id").asInt();
                String nombre = "";
                JsonNode keywords = node.get("keywords");
                if (keywords != null && keywords.size() > 0) {
                    nombre = keywords.get(0).get("keyword").asText();
                }
                resultado.add(new PictogramaDTO(id, nombre, getImagenUrl(id)));
            }
            cache.put(clave, resultado);
        } catch (Exception e) {
            System.err.println("Error buscando en ARASAAC '" + palabra + "': " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Devuelve los pictogramas de las ocho emociones básicas.
     *
     * Busca cada emoción y se queda con el primer resultado, que ARASAAC
     * considera el más relevante. Si la API no está disponible, devuelve
     * emojis Unicode como alternativa para que la pantalla no quede vacía.
     *
     * Los resultados se cachean con la clave {@code __emociones_basicas__}.
     *
     * @return lista de 8 pictogramas de emociones básicas
     */
    public List<PictogramaDTO> getEmocionesBásicas() {
        String cacheKey = "__emociones_basicas__";
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        String[][] emocionesQuery = {
                {"alegre",      "😊 Alegre"},
                {"triste",      "😢 Triste"},
                {"enfadado",    "😠 Enfadado"},
                {"miedo",       "😨 Miedo"},
                {"tranquilo",   "😌 Tranquilo"},
                {"sorprendido", "😲 Sorprendido"},
                {"cansado",     "😴 Cansado"},
                {"nervioso",    "😰 Nervioso"}
        };

        List<PictogramaDTO> resultado = new ArrayList<>();
        for (String[] emocion : emocionesQuery) {
            String query         = emocion[0];
            String labelFallback = emocion[1];
            List<PictogramaDTO> encontrados = buscar(query);
            if (!encontrados.isEmpty()) {
                PictogramaDTO primero = encontrados.get(0);
                String nombreLimpio = query.substring(0, 1).toUpperCase() + query.substring(1);
                resultado.add(new PictogramaDTO(primero.id(), nombreLimpio, primero.url()));
            } else {
                // Fallback con emoji si la API no responde
                resultado.add(new PictogramaDTO(-1, labelFallback, ""));
            }
        }

        cache.put(cacheKey, resultado);
        return resultado;
    }

    /**
     * Representa un pictograma devuelto por la API de ARASAAC.
     *
     * @param id     el identificador del pictograma en ARASAAC
     * @param nombre el nombre o etiqueta en español
     * @param url    la URL de la imagen PNG
     */
    public record PictogramaDTO(int id, String nombre, String url) {}
}