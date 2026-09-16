package cl.duoc.bancoxyz.ms.cuentas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.bancoxyz.ms.cuentas.entities.TransaccionEntity;

public interface TransaccionRepository extends JpaRepository<TransaccionEntity, Long> {
}
