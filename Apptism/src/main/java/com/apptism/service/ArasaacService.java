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
 * Servicio de integración con la API REST pública de ARASAAC
 * (Aragonese Centre of Augmentative and Alternative Communication).
 *
 * <p>Proporciona acceso a pictogramas en español utilizados en los módulos
 * de comunicación emocional y chat con pictogramas. Incorpora una caché en
 * memoria ({@link ConcurrentHashMap}) para evitar peticiones repetidas a la
 * API y reducir la latencia en la interfaz de usuario.
 *
 * <p>Las peticiones HTTP se realizan con {@link HttpClient} (Java 11+) y
 * tienen un tiempo de espera máximo de 8 segundos. Si la API no responde,
 * los métodos devuelven listas vacías o datos de fallback con emojis Unicode.
 *
 * @see <a href="https://api.arasaac.org">Documentación oficial de la API de ARASAAC</a>
 */
@Service
public class ArasaacService {

    /** URL base de la API de búsqueda de pictogramas de ARASAAC. */
    private static final String API_URL = "https://api.arasaac.org/v1/pictograms";

    /** URL base para construir las URLs de las imágenes PNG de los pictogramas. */
    private static final String IMG_URL = "https://static.arasaac.org/pictograms/";

    /** Cliente HTTP reutilizable con timeout de 8 segundos. */
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /** Mapeador JSON para deserializar las respuestas de la API. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Caché en memoria que almacena los resultados de búsquedas anteriores.
     * La clave es la palabra buscada en minúsculas; el valor, la lista de pictogramas.
     * Se usa {@link ConcurrentHashMap} para acceso seguro desde múltiples hilos.
     */
    private final ConcurrentHashMap<String, List<PictogramaDTO>> cache =
            new ConcurrentHashMap<>();

    /**
     * Construye la URL de la imagen PNG de un pictograma a partir de su identificador.
     *
     * @param id identificador numérico del pictograma en ARASAAC
     * @return URL completa de la imagen en resolución 500px
     */
    public String getImagenUrl(int id) {
        return IMG_URL + id + "/" + id + "_500.png";
    }

    /**
     * Busca pictogramas en la API de ARASAAC por palabra clave en español.
     *
     * <p>Los resultados se almacenan en caché. Si la misma palabra ya fue
     * buscada anteriormente, se devuelve el resultado cacheado sin realizar
     * ninguna petición HTTP. Se devuelven como máximo 12 resultados.
     *
     * @param palabra término de búsqueda en español
     * @return lista de hasta 12 {@link PictogramaDTO}; lista vacía si la API
     *         no está disponible o no hay resultados
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
     * Obtiene los pictogramas correspondientes a las ocho emociones básicas.
     *
     * <p>Realiza una búsqueda por cada emoción y selecciona el primer resultado,
     * que ARASAAC considera el más relevante. Si la API no está disponible,
     * devuelve un fallback con emojis Unicode para garantizar que la UI siempre
     * muestre contenido al usuario.
     *
     * <p>Los resultados se almacenan en caché con la clave
     * {@code __emociones_basicas__}.
     *
     * @return lista de 8 {@link PictogramaDTO} correspondientes a las emociones
     *         básicas; si la API falla, los pictogramas usan URL vacía y emoji como nombre
     */
    public List<PictogramaDTO> getEmocionesBásicas() {
        String cacheKey = "__emociones_basicas__";
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        // Pares [término de búsqueda, etiqueta de fallback con emoji]
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
     * Vacía la caché de pictogramas para forzar nuevas peticiones a la API.
     */
    public void limpiarCache() {
        cache.clear();
    }

    /**
     * Objeto de transferencia de datos (DTO) que representa un pictograma
     * devuelto por la API de ARASAAC.
     *
     * @param id     identificador numérico del pictograma en ARASAAC
     * @param nombre nombre o etiqueta del pictograma en español
     * @param url    URL de la imagen PNG del pictograma
     */
    public record PictogramaDTO(int id, String nombre, String url) {}
}