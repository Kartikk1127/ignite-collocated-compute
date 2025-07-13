# Apache Ignite Co-location Diagnostics & Benchmarking Tool

This project is a diagnostic and benchmarking tool for analyzing **partition co-location** in Apache Ignite 3. 
It helps you compare the performance between **colocated** and **non-colocated** joins and verify the efficiency of your data 
distribution strategies.

## Tech Stack

- **Language**: Java 17
- **Client API**: Apache Ignite 3.0.0 Client
- **Build Tool**: Maven

## Prerequisites
- Apache Ignite 3.0.0 running on ports 10800, 10801, and 10802
- JDK 17+
- Maven

## Project Structure
```bash
.
├── Readme.md
├── RunIgnite.md
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org
│   │   │       └── kartikey
│   │   │           ├── Main.java
│   │   │           └── sql
│   │   │               └── ignite.sql
│   │   └── resources
│   └── test
│       └── java
└── target
    ├── classes
    │   └── org
    │       └── kartikey
    │           └── Main.class
    └── generated-sources
        └── annotations
```

### Steps to implement the project
1. Comment everything in the main method except the `populateTables(client)` method.
2. Run the project. This will insert 100K customers and 1M orders in both orders and orders_slow table.
3. Now comment out the `populateTables(client)` method and uncomment `runDiagnostics(client)` in the main method. Run the code.
4. This will show the diagnostics:
   1. ![Screenshot from 2025-07-13 17-25-36.png](Screenshot%20from%202025-07-13%2017-25-36.png)
5. Comment out `runDiagnostics(client)` and uncomment `testPartitionDistribution(client)`. Run the code.
6. This will show the partition distribution like:
   1. ![Screenshot from 2025-07-13 17-28-34.png](Screenshot%20from%202025-07-13%2017-28-34.png)
7. Comment out `testPartitionDistribution(client)` and uncomment everything from // Warm-up phase -- //end of execution.
8. Now Run the Code again:
   1. ![Screenshot from 2025-07-13 17-08-21.png](Screenshot%20from%202025-07-13%2017-08-21.png)
9. This will show the performance of both colocated and non-colocated queries.
10. Please note, there might be some chances of non-colocated query showing better performance than colocated ones.

### Notes
1. You may sometimes see non-colocated queries performing faster than colocated ones. 
2. If this happens, increase the iteration count on line 33 to get more consistent averages.