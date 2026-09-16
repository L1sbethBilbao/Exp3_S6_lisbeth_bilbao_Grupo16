package cl.duoc.bancoxyz.semana3.config;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.Resource;

/**
 * Reader por particion: salta el header y las lineas anteriores al rango
 * (start/end en el ExecutionContext), como el PDF de rangos + @StepScope de RutaExpress.
 */
public final class LectoresCsvRango {

    private LectoresCsvRango() {
    }

    public static <T> FlatFileItemReader<T> deRango(String nombre, Resource archivo,
                                                    String[] columnas, Class<T> tipo,
                                                    int start, int end) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(columnas);

        BeanWrapperFieldSetMapper<T> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(tipo);

        DefaultLineMapper<T> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        int cantidad = Math.max(0, end - start + 1);
        return new FlatFileItemReaderBuilder<T>()
                .name(nombre)
                .resource(archivo)
                .linesToSkip(1 + start)
                .maxItemCount(cantidad)
                .lineMapper(lineMapper)
                .build();
    }

    /** Reader del CSV completo (master de remote chunking: el maestro lee y manda chunks). */
    public static <T> FlatFileItemReader<T> completo(String nombre, Resource archivo,
                                                     String[] columnas, Class<T> tipo) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(columnas);

        BeanWrapperFieldSetMapper<T> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(tipo);

        DefaultLineMapper<T> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        return new FlatFileItemReaderBuilder<T>()
                .name(nombre)
                .resource(archivo)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }
}
