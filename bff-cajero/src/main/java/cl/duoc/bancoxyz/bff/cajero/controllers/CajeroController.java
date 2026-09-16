package cl.duoc.bancoxyz.bff.cajero.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bancoxyz.bff.cajero.dtos.MovimientoCajeroDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.RetiroRequest;
import cl.duoc.bancoxyz.bff.cajero.dtos.RetiroRespuestaDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.SaldoCajeroDTO;
import cl.duoc.bancoxyz.bff.cajero.services.BffCajeroService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bff/cajero")
@RequiredArgsConstructor
public class CajeroController {

    private final BffCajeroService bffCajeroService;

    @GetMapping("/saldo/{cuentaId}")
    public SaldoCajeroDTO saldo(@PathVariable Long cuentaId) {
        return bffCajeroService.consultarSaldo(cuentaId);
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos")
    public List<MovimientoCajeroDTO> movimientos(@PathVariable Long cuentaId) {
        return bffCajeroService.ultimosMovimientos(cuentaId);
    }

    @PostMapping("/cuentas/{cuentaId}/retiro")
    public RetiroRespuestaDTO retiro(@PathVariable Long cuentaId, @RequestBody RetiroRequest request) {
        return bffCajeroService.retirar(cuentaId, request.monto());
    }
}
