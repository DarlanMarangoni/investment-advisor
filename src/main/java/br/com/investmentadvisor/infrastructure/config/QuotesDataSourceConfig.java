package br.com.investmentadvisor.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "br.com.investmentadvisor.infrastructure.adapter.out.persistence",
        entityManagerFactoryRef = "quotesEntityManagerFactory",
        transactionManagerRef = "quotesTransactionManager"
)
public class QuotesDataSourceConfig {

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.PostgreSQLDialect}")
    private String dialect;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Primary
    @Bean("quotesDataSourceProperties")
    @ConfigurationProperties("spring.datasource.quotes")
    public DataSourceProperties quotesDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean("quotesDataSource")
    public DataSource quotesDataSource(
            @Qualifier("quotesDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean("quotesEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean quotesEntityManagerFactory(
            @Qualifier("quotesDataSource") DataSource dataSource) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("br.com.investmentadvisor.infrastructure.adapter.out.persistence");
        factory.setPersistenceUnitName("quotes");
        var adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(showSql);
        factory.setJpaVendorAdapter(adapter);
        factory.setJpaPropertyMap(hibernateProperties());
        return factory;
    }

    @Primary
    @Bean("quotesTransactionManager")
    public PlatformTransactionManager quotesTransactionManager(
            @Qualifier("quotesEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private Map<String, Object> hibernateProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.format_sql", "true");
        return props;
    }
}
