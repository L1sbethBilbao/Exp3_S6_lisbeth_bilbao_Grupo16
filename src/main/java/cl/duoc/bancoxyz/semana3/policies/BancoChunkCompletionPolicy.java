package cl.duoc.bancoxyz.semana3.policies;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * CompletionPolicy personalizada: cierra el chunk cuando se alcanza
 * la cantidad (5) o el tiempo maximo, lo que ocurra primero.
 */
public class BancoChunkCompletionPolicy implements CompletionPolicy {

    private static final String ATRIB_CONTEO = "banco.chunk.conteo";
    private static final String ATRIB_INICIO = "banco.chunk.inicioMillis";

    private final int chunkSize;
    private final long maxDurationMillis;

    public BancoChunkCompletionPolicy(int chunkSize, long maxDurationMillis) {
        this.chunkSize = chunkSize;
        this.maxDurationMillis = maxDurationMillis;
    }

    @Override
    public boolean isComplete(RepeatContext context, RepeatStatus result) {
        return (result != null && !result.isContinuable()) || isComplete(context);
    }

    @Override
    public boolean isComplete(RepeatContext context) {
        int conteo = (int) context.getAttribute(ATRIB_CONTEO);
        long inicio = (long) context.getAttribute(ATRIB_INICIO);
        boolean porCantidad = conteo >= chunkSize;
        boolean porTiempo = (System.currentTimeMillis() - inicio) >= maxDurationMillis;
        return porCantidad || porTiempo;
    }

    @Override
    public RepeatContext start(RepeatContext parent) {
        RepeatContextSupport context = new RepeatContextSupport(parent);
        context.setAttribute(ATRIB_CONTEO, 0);
        context.setAttribute(ATRIB_INICIO, System.currentTimeMillis());
        return context;
    }

    @Override
    public void update(RepeatContext context) {
        int conteo = (int) context.getAttribute(ATRIB_CONTEO);
        context.setAttribute(ATRIB_CONTEO, conteo + 1);
    }
}
