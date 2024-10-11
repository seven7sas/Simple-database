package ru.giga.dev.database.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import ru.giga.dev.MyObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RegisterRowMapper(MyObject.RowMapper.class)
public interface DatabaseDao {
    @SqlUpdate("CREATE TABLE IF NOT EXISTS objects (id TEXT PRIMARY KEY, text TEXT)")
    void createTable();

    @SqlUpdate("INSERT INTO objects (id, text) VALUES (:id, :text)")
    void insert(@Bind("id") UUID id, @Bind("text") String text);

    @SqlUpdate("UPDATE objects SET text = :text WHERE id = :id")
    void update(@Bind("id") UUID id, @Bind("text") String text);

    @SqlUpdate("DELETE FROM objects WHERE id = :id")
    void delete(@Bind("id") UUID id);

    @SqlQuery("SELECT * FROM objects")
    List<MyObject> list();

    default Map<UUID, String> map() {
        return list().stream().collect(Collectors.toMap(MyObject::getId, MyObject::getText));
    }
}
