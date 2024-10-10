package ru.giga.dev;

import ru.giga.dev.database.Database;

public class Testing {
    public static void main(String[] args) {
        Database database = new Database("database", 8);
    }
}
