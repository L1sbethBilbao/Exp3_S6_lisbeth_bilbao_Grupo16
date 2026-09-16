package cl.duoc.bancoxyz.bff.cajero.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MsCuentasClientConfig {

    @Bean
    public ClientHttpRequestFactory backendRequestFactory(
            @Value("${backend.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${backend.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }

    @Bean
    public RestClient msCuentasRestClient(@Value("${ms-cuentas.base-url}") String baseUrl,
                                          ClientHttpRequestFactory backendRequestFactory) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(backendRequestFactory).build();
    }
}
