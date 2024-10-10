package ru.giga.dev.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import ru.giga.dev.database.dao.DatabaseDao;

import java.util.function.Consumer;

public class Database {

    private final Jdbi jdbi;
    private final HikariDataSource dataSource;
    private final DatabaseDao dao;

    public Database(Jdbi jdbi, HikariDataSource dataSource, DatabaseDao dao) {
        this.jdbi = jdbi;
        this.dataSource = dataSource;
        this.dao = dao;
    }

    public Database(String name, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + name + ".db");
        config.setMaximumPoolSize(maxPoolSize);
        dataSource = new HikariDataSource(config);
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        dao = jdbi.withExtension(DatabaseDao.class, extension -> extension);
    }

    public void execute(Consumer<DatabaseDao> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer cannot be null");
        }
        jdbi.useExtension(DatabaseDao.class, consumer::accept);
    }

    public DatabaseDao getDao() {
        return dao;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    public void shutdown() {
        dataSource.close();
    }
}
