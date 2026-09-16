package cl.duoc.bancoxyz.ms.cuentas.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bancoxyz.ms.cuentas.dtos.CuentaCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.dtos.MovimientoCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.dtos.RetiroRequest;
import cl.duoc.bancoxyz.ms.cuentas.dtos.TransaccionCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.services.BancoCoreService;
import lombok.RequiredArgsConstructor;

/**
 * API interna completa: el mismo JSON para cualquier consumidor.
 * Los frontends no deben llamar aqui; lo hacen los BFF.
 */
@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class BancoCoreController {

    private final BancoCoreService bancoCoreService;

    @GetMapping("/cuentas")
    public List<CuentaCoreDTO> listarCuentas() {
        return bancoCoreService.listarCuentas();
    }

    @GetMapping("/cuentas/{cuentaId}")
    public CuentaCoreDTO obtenerCuenta(@PathVariable Long cuentaId) {
        return bancoCoreService.obtenerCuenta(cuentaId);
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos")
    public List<MovimientoCoreDTO> listarMovimientos(@PathVariable Long cuentaId) {
        return bancoCoreService.listarMovimientos(cuentaId);
    }

    @GetMapping("/transacciones")
    public List<TransaccionCoreDTO> listarTransacciones() {
        return bancoCoreService.listarTransacciones();
    }

    @PostMapping("/cuentas/{cuentaId}/retiro")
    public CuentaCoreDTO retirar(@PathVariable Long cuentaId, @RequestBody RetiroRequest request) {
        return bancoCoreService.retirar(cuentaId, request.monto());
    }
}
