package ru.giga.dev;

import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class MyObject {
    private final UUID id;
    private String text;

    public MyObject(UUID id, String text) {
        this.id = id;
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyObject myObject = (MyObject) o;
        return Objects.equals(id, myObject.id) && Objects.equals(text, myObject.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text);
    }

    @Override
    public String toString() {
        return "MyObject{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }

    public static class RowMapper implements org.jdbi.v3.core.mapper.RowMapper<MyObject> {
        @Override
        public MyObject map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new MyObject(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("text")
            );
        }
    }
}
