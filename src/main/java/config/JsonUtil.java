package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public final class JsonUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private JsonUtil() { }

    public static <T> T ler(HttpServletRequest request, Class<T> tipo) throws IOException {
        return MAPPER.readValue(request.getInputStream(), tipo);
    }

    public static void enviar(HttpServletResponse response, int status, Object corpo) throws IOException {
        response.setStatus(status); response.setCharacterEncoding("UTF-8"); response.setContentType("application/json");
        MAPPER.writeValue(response.getOutputStream(), corpo);
    }

    public static void erro(HttpServletResponse response, int status, String mensagem) throws IOException {
        enviar(response, status, Map.of("erro", mensagem));
    }
}
