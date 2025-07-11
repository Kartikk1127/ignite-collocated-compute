package org.kartikey;

import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static class Person {
        // Default constructor required for serialization
        public Person() { }

        public Person(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        Integer id;
        String name;
    }
    public static void main(String[] args) {
        String [] addresses = {
                "127.0.0.1:10800",
                "127.0.0.1:10801",
                "127.0.0.1:10802"
        };

        try (IgniteClient client = IgniteClient.builder().addresses(addresses).build()) {
            System.out.println("Connected to the cluster: " + client.connections());

            queryExistingTable(client);

            Table table = createTable(client);

            populateTableWithDifferentViews(table);

            queryNewTable(client);
        }
    }

    private static void queryExistingTable(IgniteClient client) {
        System.out.println("Querying Person table");
        client.sql().execute(null,"Select * from Person")
                .forEachRemaining(row -> System.out.println("Person: " + row.stringValue("name")));
    }

    private static Table createTable(IgniteClient client) {
        System.out.println("Creating Person2 table");
        return client.catalog().createTable(TableDefinition.builder("Person2")
                .ifNotExists()
                .columns(
                        ColumnDefinition.column("ID", ColumnType.INT32),
                        ColumnDefinition.column("NAME", ColumnType.VARCHAR)
                )
                .primaryKey("ID")
                .build());
    }

    private static void populateTableWithDifferentViews(Table table) {
        System.out.println("\n--- Populating Person2 table using different views ---");

        // 1. Using RecordView with Tuples
        RecordView<Tuple> recordView = table.recordView();
        recordView.upsert(null, Tuple.create().set("id", 2).set("name", "Jane"));
        System.out.println("Added record using RecordView with Tuple");

        // 2. Using RecordView with POJOs
        RecordView<Person> pojoView = table.recordView(Person.class);
        pojoView.upsert(null, new Person(3, "Jack"));
        System.out.println("Added record using RecordView with POJO");

        // 3. Using KeyValueView with Tuples
        KeyValueView<Tuple, Tuple> keyValueView = table.keyValueView();
        keyValueView.put(null, Tuple.create().set("id", 4), Tuple.create().set("name", "Jill"));
        System.out.println("Added record using KeyValueView with Tuples");

        // 4. Using KeyValueView with Native Types
        KeyValueView<Integer, String> keyValuePojoView = table.keyValueView(Integer.class, String.class);
        keyValuePojoView.put(null, 5, "Joe");
        System.out.println("Added record using KeyValueView with Native Types");
    }

    private static void queryNewTable(IgniteClient client) {
        System.out.println("\n--- Querying Person2 table ---");
        client.sql().execute(null, "SELECT * FROM Person2")
                .forEachRemaining(row -> System.out.println("Person2: " + row.stringValue("name")));
    }
}