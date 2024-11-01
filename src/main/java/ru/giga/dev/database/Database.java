package ru.giga.dev.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import ru.giga.dev.database.dao.Dao;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Database {

    public final static ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("Database IO #%d").build();
    private final Jdbi jdbi;
    private final JdbiExecutor executor;
    private final HikariDataSource dataSource;

    public Database(Jdbi jdbi, JdbiExecutor executor, HikariDataSource dataSource) {
        this.jdbi = jdbi;
        this.executor = executor;
        this.dataSource = dataSource;
    }

    public Database(String name, int idAsync) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + name + ".db");
        dataSource = new HikariDataSource(config);
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new CaffeineCachePlugin());
        ExecutorService executorService = readIdAsync(idAsync);
        executor = JdbiExecutor.create(jdbi, executorService);
    }

    public Database(String name, int idAsync, String host, String port, String username, String password, String flags) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name + "?" + flags);
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new CaffeineCachePlugin());
        ExecutorService executorService = readIdAsync(idAsync);
        executor = JdbiExecutor.create(jdbi, executorService);
    }

    public static ExecutorService readIdAsync(int idAsync) {
        return switch (idAsync) {
            case 0 ->
                // Создает пул потоков с поддержкой планирования, позволяет выполнять задачи с задержкой или периодически.
                    Executors.newScheduledThreadPool(8, THREAD_FACTORY);
            case 1 ->
                // Поддержка динамического управления потоками: при бездействии потоки могут быть завершены, использует очередь заданий для хранения задач перед их выполнением.
                    new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() / 2,
                            60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(),
                            THREAD_FACTORY
                    );
            case 2 ->
                // Создает фиксированный пул потоков с указанным количеством потоков. Полезно для выполнения задач параллельно с ограниченным числом потоков.
                    Executors.newFixedThreadPool(
                            Math.max(2, Runtime.getRuntime().availableProcessors() - 1), // как минимум 2 потока
                            THREAD_FACTORY
                    );
            case 3 ->
                // Одинокий поток, выполняющий задачи последовательно.
                    Executors.newSingleThreadExecutor(THREAD_FACTORY);
            default -> throw new IllegalStateException("Id async " + idAsync + " not found");
        };
    }

    public void useExecute(boolean async, Consumer<Dao> consumer) {
        if (consumer == null) throw new IllegalArgumentException("Consumer cannot be null");
        if (async) executor.useExtension(Dao.class, consumer::accept);
        else jdbi.useExtension(Dao.class, consumer::accept);
    }

    public <R> R withExecute(boolean async, Function<Dao, R> function) {
        if (function == null) throw new IllegalArgumentException("Function cannot be null");
        if (async) return executor.withExtension(Dao.class, function::apply).toCompletableFuture().join();
        else return jdbi.withExtension(Dao.class, function::apply);
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
