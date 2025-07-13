package org.kartikey;

import org.apache.ignite.client.IgniteClient;

import java.util.Objects;
import java.util.Random;
// To run the project locally, read and follow the RunIgnite.md first and then Readme.md

public class Main {
    private static final Random random = new Random();

    public static void main(String[] args) {
        String[] addresses = {
                "127.0.0.1:10800",
                "127.0.0.1:10801",
                "127.0.0.1:10802"
        };

        try (IgniteClient client = IgniteClient.builder().addresses(addresses).build()) {
//            populateTables(client);
//            runDiagnostics(client);
//            testPartitionDistribution(client);
            // Warm-up phase
            System.out.println("Starting the warm-up period...");
            for (int i = 0; i < 10; i++) {
                runColocatedQuery(client);
                runNonColocatedQuery(client);
            }
            System.out.println("Tables warmed up.");

            // Benchmark colocated query
            System.out.println(" Benchmarking Colocated Query ");
            long colocatedSum = 0;
            long iterationCount = 200;
            for (int i = 0; i < iterationCount; i++) {
                colocatedSum += runColocatedQuery(client);
            }
            long colocatedAvg = colocatedSum / iterationCount;

            // Benchmark non-colocated query
            System.out.println("Benchmarking Non-Colocated Query ");
            long nonColocatedSum = 0;
            for (int i = 0; i < iterationCount; i++) {
                nonColocatedSum += runNonColocatedQuery(client);
            }
            long nonColocatedAvg = nonColocatedSum / iterationCount;

            // Results
            System.out.println(" BENCHMARK RESULTS =>");
            System.out.println("Colocated query average time: " + colocatedAvg + " ms");
            System.out.println("Non-colocated query average time: " + nonColocatedAvg + " ms");

            if (colocatedAvg < nonColocatedAvg) {
                System.out.println("Colocation is working! Colocated is faster by " + (double) nonColocatedAvg/colocatedAvg + " times");
                System.out.println();
            } else {
                System.out.println("Colocation not working. Non-colocated is faster by " + (double) colocatedAvg/nonColocatedAvg + " times");
            }
            //end of execution
        }
    }

    private static void runDiagnostics(IgniteClient client) {
        System.out.println(" DIAGNOSTIC INFORMATION ");

        System.out.println(" ZONES ");
        client.sql().execute(null, "SELECT * FROM SYSTEM.ZONES")
                .forEachRemaining(row -> {
                    System.out.println("Zone: " + row.value(0) + ", Partitions: " + row.value(1) +
                            ", Replicas: " + row.value(2));
                });

        System.out.println(" TABLES ");
        client.sql().execute(null,
                        "SELECT NAME, ZONE FROM SYSTEM.TABLES WHERE NAME IN ('CUSTOMERS', 'ORDERS', 'ORDERS_SLOW')")
                .forEachRemaining(row -> {
                    System.out.println("Table: " + row.value(0) + ", Zone: " + row.value(1));
                });

        System.out.println(" EXECUTION PLAN FOR ORDERS ");
        client.sql().execute(null,
                        "EXPLAIN PLAN FOR SELECT SUM(o.amount) FROM Orders o JOIN Customers c ON o.CustomerId = c.CustomerId WHERE c.CustomerId = 1")
                .forEachRemaining(row -> {
                    System.out.println(row.value(0).toString());
                });

        System.out.println(" EXECUTION PLAN FOR ORDERS_SLOW ");
        client.sql().execute(null,
                        "EXPLAIN PLAN FOR SELECT SUM(o.amount) FROM Orders_Slow o JOIN Customers c ON o.CustomerId = c.CustomerId WHERE c.CustomerId = 1")
                .forEachRemaining(row -> {
                    System.out.println(row.value(0).toString());
                });

        System.out.println(" INDEXES ");
        client.sql().execute(null,
                        "SELECT TABLE_NAME, INDEX_NAME, COLUMNS FROM SYSTEM.INDEXES WHERE TABLE_NAME IN ('CUSTOMERS', 'ORDERS', 'ORDERS_SLOW')")
                .forEachRemaining(row -> {
                    System.out.println("Table: " + row.value(0) + ", Index: " + row.value(1) +
                            ", Columns: " + row.value(2));
                });
    }

    private static void testPartitionDistribution(IgniteClient client) {
        System.out.println("\n=== PARTITION DISTRIBUTION TEST ===");

        // Test a few specific customers to see where their data is
        int[] testCustomers = {1, 100, 500, 999};

        for (int customerId : testCustomers) {
            System.out.println("\n--- Testing CustomerId: " + customerId + " ---");

            // Count orders for this customer in both tables
            var ordersCount = client.sql().execute(null,
                    "SELECT COUNT(*) FROM Orders WHERE CustomerId = ?", customerId);
            var ordersSlowCount = client.sql().execute(null,
                    "SELECT COUNT(*) FROM Orders_Slow WHERE CustomerId = ?", customerId);

            System.out.println("Orders count: " + ordersCount.next().value(0));
            System.out.println("Orders_Slow count: " + ordersSlowCount.next().value(0));

            // Time individual queries
            long start = System.nanoTime();
            client.sql().execute(null,
                    "SELECT SUM(o.amount) FROM Orders o JOIN Customers c ON o.CustomerId = c.CustomerId WHERE c.CustomerId = ?",
                    customerId).forEachRemaining(row -> {});
            long ordersTime = (System.nanoTime() - start) / 1_000_000;

            start = System.nanoTime();
            client.sql().execute(null,
                    "SELECT SUM(o.amount) FROM Orders_Slow o JOIN Customers c ON o.CustomerId = c.CustomerId WHERE c.CustomerId = ?",
                    customerId).forEachRemaining(row -> {});
            long ordersSlowTime = (System.nanoTime() - start) / 1_000_000;

            System.out.println("Orders query time: " + ordersTime + " ms");
            System.out.println("Orders_Slow query time: " + ordersSlowTime + " ms");
        }
    }


    private static long runColocatedQuery(IgniteClient client) {
        int customerId = random.nextInt(1000) + 1; // Random customer 1-1000
        long start = System.nanoTime();
        executeQuery(client, "Orders", customerId);
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static long runNonColocatedQuery(IgniteClient client) {
        int customerId = random.nextInt(1000) + 1; // Random customer 1-1000
        long start = System.nanoTime();
        executeQuery(client, "Orders_Slow", customerId);
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static void executeQuery(IgniteClient client, String tableName, int customerId) {
        String query = "SELECT SUM(o.amount) FROM " + tableName +
                " o JOIN Customers c ON o.CustomerId = c.CustomerId WHERE c.CustomerId = ?";

        client.sql().execute(null, query, customerId)
                .forEachRemaining(row -> {
                }); // Consume result
    }

    private static void populateTables(IgniteClient client) {
        System.out.println(" Populating Customers and Orders Tables with Bulk Inserts ");

        int totalCustomers = 100_000;
        int totalOrders = 1_000_000;
        int batchSize = 200;
        Random random = new Random();

        // Insert Customers
//        System.out.println("Inserting " + totalCustomers + " customers...");
//        for (int i = 1; i <= totalCustomers; i += batchSize) {
//            StringBuilder stmt = new StringBuilder("INSERT INTO Customers (CustomerId) VALUES ");
//            for (int j = 0; j < batchSize && (i + j) <= totalCustomers; j++) {
//                int customerId = i + j;
//                stmt.append("(").append(customerId).append(")");
//                if (j != batchSize - 1 && (i + j + 1) <= totalCustomers) stmt.append(", ");
//            }
//            client.sql().execute(null, stmt.toString());
//            sleepBetweenBatches(random);
//            if (i % 5000 == 1) {
//                System.out.println("Inserted Customers: " + Math.min(i + batchSize - 1, totalCustomers));
//            }
//        }
//        System.out.println("✅ Customers inserted.");

        // Insert Orders and Orders_Slow
        System.out.println("Inserting " + totalOrders + " orders into Orders and Orders_Slow...");
        for (int i = 1; i <= totalOrders; i += batchSize) {
            StringBuilder ordersStmt = new StringBuilder("INSERT INTO Orders (OrderId, CustomerId, Amount) VALUES ");
            StringBuilder ordersSlowStmt = new StringBuilder("INSERT INTO Orders_Slow (OrderId, CustomerId, Amount) VALUES ");

            for (int j = 0; j < batchSize && (i + j) <= totalOrders; j++) {
                int orderId = i + j;
                int customerId = (orderId % totalCustomers) + 1;
                double amount = Math.round(random.nextDouble() * 1000 * 100.0) / 100.0;

                ordersStmt.append("(").append(orderId).append(", ").append(customerId).append(", ").append(amount).append(")");
                ordersSlowStmt.append("(").append(orderId).append(", ").append(customerId).append(", ").append(amount).append(")");

                if (j != batchSize - 1 && (i + j + 1) <= totalOrders) {
                    ordersStmt.append(", ");
                    ordersSlowStmt.append(", ");
                }
            }

            client.sql().execute(null, ordersStmt.toString());
            client.sql().execute(null, ordersSlowStmt.toString());

            if (i % 5000 == 1) {
                System.out.println("Inserted Orders: " + Math.min(i + batchSize - 1, totalOrders));
            }

            sleepBetweenBatches(random);
        }

        System.out.println("✅ Orders inserted into both tables.");

        // Final counts
        System.out.println("\n--- Verifying Final Counts ---");
        var c = client.sql().execute(null, "SELECT COUNT(*) FROM Customers").next().value(0);
        var o = client.sql().execute(null, "SELECT COUNT(*) FROM Orders").next().value(0);
        var os = client.sql().execute(null, "SELECT COUNT(*) FROM Orders_Slow").next().value(0);

        System.out.println("Customers count: " + c);
        System.out.println("Orders count: " + o);
        System.out.println("Orders_Slow count: " + os);
    }

    private static void sleepBetweenBatches(Random random) {
        try {
            int delay = 1000 + random.nextInt(1000); // 1 to 2 seconds
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
