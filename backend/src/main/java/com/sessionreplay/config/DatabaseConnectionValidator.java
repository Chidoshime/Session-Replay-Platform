package com.sessionreplay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Компонент для проверки подключения к базе данных при старте приложения.
 * Выполняет простой запрос и логирует результат.
 */
@Component
@Slf4j
public class DatabaseConnectionValidator implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionValidator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) {
        log.info("🔍 Проверка подключения к базе данных...");
        
        try {
            // Получаем информацию о подключении
            var connection = dataSource.getConnection();
            String dbUrl = connection.getMetaData().getURL();
            String dbName = connection.getCatalog();
            String dbUser = connection.getMetaData().getUserName();
            connection.close();
            
            log.info("✅ Успешное подключение к БД!");
            log.info("   URL: {}", dbUrl);
            log.info("   База данных: {}", dbName);
            log.info("   Пользователь: {}", dbUser);
            
            // Выполняем тестовый запрос
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            if (result != null && result == 1) {
                log.info("✅ Тестовый запрос выполнен успешно (SELECT 1 = {})", result);
                log.info("🚀 База данных готова к работе!");
            } else {
                log.error("❌ Тестовый запрос вернул неожиданный результат: {}", result);
                throw new RuntimeException("Некорректный ответ от базы данных");
            }
            
        } catch (Exception e) {
            log.error("❌ Ошибка подключения к базе данных!", e);
            log.error("💡 Проверьте:");
            log.error("   1. Доступность сервера БД (10.5.92.108:5432)");
            log.error("   2. Корректность учетных данных (логин/пароль)");
            log.error("   3. Существование базы данных 'session_replay_db'");
            log.error("   4. Настройки брандмауэра и сетевого доступа");
            
            // Пробрасываем исключение, чтобы приложение не запустилось без БД
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        }
    }
}
