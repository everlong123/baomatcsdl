package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    
    @Value("${spring.datasource.admin.url}")
    private String adminUrl;
    
    @Value("${spring.datasource.admin.username}")
    private String adminUsername;
    
    @Value("${spring.datasource.admin.password}")
    private String adminPassword;
    
    @Value("${spring.datasource.app.url}")
    private String appUrl;
    
    @Value("${spring.datasource.app.username}")
    private String appUsername;
    
    @Value("${spring.datasource.app.password}")
    private String appPassword;
    
    @Bean(name = "adminDataSource")
    public DataSource adminDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(adminUrl);
        config.setUsername(adminUsername);
        config.setPassword(adminPassword);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        return new HikariDataSource(config);
    }
    
    @Bean(name = "appDataSource")
    public DataSource appDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(appUrl);
        config.setUsername(appUsername);
        config.setPassword(appPassword);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        return new HikariDataSource(config);
    }
    
    @Bean(name = "adminJdbcTemplate")
    @Primary
    public JdbcTemplate adminJdbcTemplate(@Qualifier("adminDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    @Bean(name = "appJdbcTemplate")
    public JdbcTemplate appJdbcTemplate(@Qualifier("appDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

