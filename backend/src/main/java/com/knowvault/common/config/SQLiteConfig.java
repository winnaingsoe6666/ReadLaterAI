package com.knowvault.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

@Configuration
public class SQLiteConfig {

    @Bean
    public DataSource dataSource(DataSource original) {
        return new org.springframework.jdbc.datasource.DelegatingDataSource(original) {
            @Override
            public Connection getConnection() throws SQLException {
                Connection conn = super.getConnection();
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
                return conn;
            }
        };
    }
}
