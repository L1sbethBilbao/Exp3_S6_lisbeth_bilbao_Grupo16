package cl.duoc.bancoxyz.bff.movil.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autenticacion (token del canal) y autorizacion (permisos MOVIL) del BFF Movil.
 */
@Component
public class CanalAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_CANAL = "bancoxyz.canal";
    public static final String ATTR_PERMISOS = "bancoxyz.permisos";

    @Value("${bancoxyz.auth.token}")
    private String tokenEsperado;

    @Value("${bancoxyz.auth.canal}")
    private String canal;

    @Value("${bancoxyz.auth.permisos}")
    private String permisosConfigurados;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("X-Canal-Token");
        if (!tokenEsperado.equals(token)) {
            escribirError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token del canal movil invalido o ausente");
            return;
        }

        Set<String> permisos = Arrays.stream(permisosConfigurados.split(","))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toSet());

        if (!permisos.contains("CONSULTA_LIVIANA")) {
            escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Canal MOVIL sin permiso CONSULTA_LIVIANA");
            return;
        }

        request.setAttribute(ATTR_CANAL, canal);
        request.setAttribute(ATTR_PERMISOS, permisos);
        filterChain.doFilter(request, response);
    }

    private void escribirError(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + mensaje + "\",\"canal\":\"" + canal + "\"}");
    }
}
