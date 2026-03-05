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

@Service
public class ArasaacService {

    private static final String API_URL = "https://api.arasaac.org/v1/pictograms";
    private static final String IMG_URL = "https://static.arasaac.org/pictograms/";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    // Caché en memoria para evitar repetir búsquedas idénticas
    private final ConcurrentHashMap<String, List<PictogramaDTO>> cache = new ConcurrentHashMap<>();

    public String getImagenUrl(int id) {
        return IMG_URL + id + "/" + id + "_500.png";
    }

    /**
     * Busca pictogramas en la API de ARASAAC por palabra en español.
     * Cachea resultados para no saturar la API.
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
                    .GET().build();
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
     * Emociones básicas cargadas dinámicamente desde la API.
     * Busca cada emoción y toma el primer resultado válido.
     * Si la API no está disponible, usa fallback con emojis Unicode.
     */
    public List<PictogramaDTO> getEmocionesBásicas() {
        // Usar caché si ya se buscaron
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
            String query = emocion[0];
            String labelFallback = emocion[1];
            List<PictogramaDTO> encontrados = buscar(query);
            if (!encontrados.isEmpty()) {
                // Tomar el primer resultado — es el más relevante según ARASAAC
                PictogramaDTO primero = encontrados.get(0);
                // Usar el nombre en español limpio (sin emoji)
                String nombreLimpio = query.substring(0,1).toUpperCase() + query.substring(1);
                resultado.add(new PictogramaDTO(primero.id(), nombreLimpio, primero.url()));
            } else {
                // Fallback: placeholder con emoji si la API no responde
                resultado.add(new PictogramaDTO(-1, labelFallback, ""));
            }
        }

        cache.put(cacheKey, resultado);
        return resultado;
    }

    /** Limpia la caché (útil si se quiere refrescar) */
    public void limpiarCache() {
        cache.clear();
    }

    public record PictogramaDTO(int id, String nombre, String url) {}
}
