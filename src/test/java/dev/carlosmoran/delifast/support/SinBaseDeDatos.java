package dev.carlosmoran.delifast.support;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * Arranca la aplicación completa sin PostgreSQL: con DataSource, JPA y Flyway excluidos
 * (ver {@link #SIN_BD}), cada repositorio de Spring Data se sustituye por un mock de Mockito
 * y hay un gestor de transacciones de mentira. Sirve para lo que no toca datos, como
 * generar la documentación OpenAPI.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SinBaseDeDatos {

    public static final String SIN_BD = "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
            + "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration,"
            + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration";

    @Bean
    static BeanDefinitionRegistryPostProcessor repositoriosDeMentira() {
        return (BeanDefinitionRegistry registro) -> {
            ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition definicion) {
                    return definicion.getMetadata().isInterface();
                }
            };
            escaner.addIncludeFilter(new AssignableTypeFilter(Repository.class));
            for (BeanDefinition candidato : escaner.findCandidateComponents("dev.carlosmoran.delifast.repository")) {
                Class<?> tipo = ClassUtils.resolveClassName(candidato.getBeanClassName(), SinBaseDeDatos.class.getClassLoader());
                registro.registerBeanDefinition(tipo.getSimpleName(), mock(tipo));
            }
        };
    }

    @Bean
    PlatformTransactionManager transactionManager() {
        return Mockito.mock(PlatformTransactionManager.class);
    }

    private static <T> RootBeanDefinition mock(Class<T> tipo) {
        return new RootBeanDefinition(tipo, () -> Mockito.mock(tipo));
    }
}
