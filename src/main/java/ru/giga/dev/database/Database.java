package ru.giga.dev.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import ru.giga.dev.database.dao.DatabaseDao;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class Database {

    private final Jdbi jdbi;
    private final JdbiExecutor executor;
    private final HikariDataSource dataSource;

    public Database(Jdbi jdbi, JdbiExecutor executor, HikariDataSource dataSource) {
        this.jdbi = jdbi;
        this.executor = executor;
        this.dataSource = dataSource;
    }

    public Database(String name, boolean defaultAsync) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + name + ".db");
        dataSource = new HikariDataSource(config);
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new CaffeineCachePlugin());
        ExecutorService executorService;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Database IO #%d").build();
        if (defaultAsync) executorService = Executors.newScheduledThreadPool(8, threadFactory);
        else executorService = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() / 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
        executor = JdbiExecutor.create(jdbi, executorService);
    }

    public Database(String name, boolean defaultAsync, String host, String port, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new CaffeineCachePlugin());
        ExecutorService executorService;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Database IO #%d").build();
        if (defaultAsync) executorService = Executors.newScheduledThreadPool(8, threadFactory);
        else executorService = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() / 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
        executor = JdbiExecutor.create(jdbi, executorService);
    }


    public void execute(boolean async, Consumer<DatabaseDao> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer cannot be null");
        }
        if (async) executor.useExtension(DatabaseDao.class, consumer::accept);
        else jdbi.useExtension(DatabaseDao.class, consumer::accept);
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    public JdbiExecutor getExecutor() {
        return executor;
    }

    public void shutdown() {
        dataSource.close();
    }
}
