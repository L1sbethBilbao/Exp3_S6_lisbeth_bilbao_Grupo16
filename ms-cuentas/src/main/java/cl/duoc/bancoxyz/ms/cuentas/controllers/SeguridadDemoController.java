package cl.duoc.bancoxyz.ms.cuentas.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bancoxyz.ms.cuentas.dtos.CuentaCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.services.BancoCoreService;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints de seguridad Semana 6 (estilo clase): publico, Basic y OAuth2/JWT.
 */
@RestController
@RequiredArgsConstructor
public class SeguridadDemoController {

    private final BancoCoreService bancoCoreService;

    @Value("${bancoxyz.mensaje:Config local (Config Server no disponible)}")
    private String mensajeConfig;

    @Value("${bancoxyz.ambiente:local}")
    private String ambiente;

    @GetMapping("/api/publico/info")
    public Map<String, Object> infoPublica() {
        return Map.of(
                "mensaje", "Endpoint publico, no requiere autenticacion",
                "service", "ms-cuentas",
                "configDesdeConfigServer", mensajeConfig,
                "ambiente", ambiente);
    }

    @GetMapping("/api/basic/cuentas")
    public List<CuentaCoreDTO> cuentasBasic(Authentication authentication) {
        return bancoCoreService.listarCuentas();
    }

    @GetMapping("/api/basic/admin/cuentas")
    public Map<String, Object> resumenAdmin() {
        List<CuentaCoreDTO> cuentas = bancoCoreService.listarCuentas();
        return Map.of(
                "totalCuentas", cuentas.size(),
                "mensaje", "Solo rol ADMIN",
                "autenticadoComo", "admin");
    }

    @GetMapping("/api/oauth2/cuentas")
    public Map<String, Object> cuentasOAuth2(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "cuentas", bancoCoreService.listarCuentas(),
                "autenticadoComo", jwt.getSubject(),
                "scope", jwt.getClaimAsString("scope"));
    }
}
