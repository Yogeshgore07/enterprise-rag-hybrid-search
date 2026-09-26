package com.yogesh.ragassistant.config;

import com.yogesh.ragassistant.entity.Role;
import com.yogesh.ragassistant.entity.User;
import com.yogesh.ragassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Automates initial database setup without Flyway:
 * 1. Enables pgvector extension (if on PostgreSQL)
 * 2. Creates full-text search trigger and indexes
 * 3. Seeds default ADMIN and USER accounts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking database configuration and initializing extensions...");

        try {
            // Check if we are running on PostgreSQL
            String databaseProductName = jdbcTemplate.execute(
                    (java.sql.Connection conn) -> conn.getMetaData().getDatabaseProductName()
            );

            if (databaseProductName != null && databaseProductName.toLowerCase().contains("postgres")) {
                log.info("PostgreSQL detected. Initializing pgvector extension and search vector trigger...");

                // 1. Enable extensions
                executeSqlQuietly("pgvector extension", "CREATE EXTENSION IF NOT EXISTS vector;");
                executeSqlQuietly("pgcrypto extension", "CREATE EXTENSION IF NOT EXISTS pgcrypto;");

                // 2. Ensure search_vector column has type tsvector (Hibernate ddl-auto=update may have created it as varchar)
                executeSqlQuietly("ensure search_vector column is tsvector", """
                    DO $$
                    BEGIN
                        IF EXISTS (
                            SELECT 1 FROM information_schema.columns 
                            WHERE table_name = 'document_chunks' 
                            AND column_name = 'search_vector' 
                            AND udt_name != 'tsvector'
                        ) THEN
                            ALTER TABLE document_chunks DROP COLUMN search_vector CASCADE;
                            ALTER TABLE document_chunks ADD COLUMN search_vector tsvector;
                        END IF;
                    END $$;
                """);

                // 3. Ensure created_at has default CURRENT_TIMESTAMP
                executeSqlQuietly("set created_at default timestamp", """
                    DO $$
                    BEGIN
                        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'document_chunks') THEN
                            ALTER TABLE document_chunks ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
                        END IF;
                    END $$;
                """);

                // 4. Trigger function for tsvector search_vector
                executeSqlQuietly("search_vector trigger function", """
                    CREATE OR REPLACE FUNCTION document_chunks_search_vector_trigger() RETURNS trigger AS $$
                    BEGIN
                        NEW.search_vector := to_tsvector('english', COALESCE(NEW.chunk_text, ''));
                        RETURN NEW;
                    END
                    $$ LANGUAGE plpgsql;
                """);

                // 3. Attach trigger if table exists
                executeSqlQuietly("search_vector trigger", """
                    DO $$
                    BEGIN
                        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'document_chunks') THEN
                            IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_document_chunks_search_vector') THEN
                                CREATE TRIGGER trg_document_chunks_search_vector
                                BEFORE INSERT OR UPDATE OF chunk_text ON document_chunks
                                FOR EACH ROW EXECUTE FUNCTION document_chunks_search_vector_trigger();
                            END IF;
                        END IF;
                    END $$;
                """);

                // 4. Create HNSW and GIN indexes
                executeSqlQuietly("full-text search GIN index",
                    "CREATE INDEX IF NOT EXISTS idx_chunks_search_vector ON document_chunks USING gin(search_vector);");
                executeSqlQuietly("vector HNSW index",
                    "CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON document_chunks USING hnsw (embedding vector_cosine_ops);");
            }
        } catch (Exception ex) {
            log.warn("Database initialization script notice: {}", ex.getMessage());
        }

        // Seed default admin and user if repository is empty
        seedDefaultUsers();
    }

    private void executeSqlQuietly(String description, String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Successfully executed database setup for: {}", description);
        } catch (Exception ex) {
            log.warn("Database initialization notice for '{}': {}", description, ex.getMessage());
        }
    }

    private void seedDefaultUsers() {
        try {
            if (!userRepository.existsByEmail("admin@company.com")) {
                log.info("Seeding default ADMIN user: admin@company.com");
                User admin = User.builder()
                        .email("admin@company.com")
                        .password(passwordEncoder.encode("Password@123"))
                        .fullName("System Administrator")
                        .role(Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
            }

            if (!userRepository.existsByEmail("user@company.com")) {
                log.info("Seeding default USER: user@company.com");
                User user = User.builder()
                        .email("user@company.com")
                        .password(passwordEncoder.encode("Password@123"))
                        .fullName("Enterprise Knowledge Worker")
                        .role(Role.ROLE_USER)
                        .build();
                userRepository.save(user);
            }
        } catch (Exception ex) {
            log.warn("Could not seed default users: {}", ex.getMessage());
        }
    }
}
