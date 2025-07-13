1. `CREATE ZONE IF NOT EXISTS CUSTOMER WITH replicas=3, partitions=5, storage_profiles='default';`
2. `CREATE ZONE IF NOT EXISTS ORDERS WITH replicas=2, partitions=5, storage_profiles='default';`
3. `CREATE TABLE IF NOT EXISTS Customers(CustomerId INT NOT NULL, Name VARCHAR, PRIMARY KEY (CustomerId) ) ZONE CUSTOMER;`
4. `CREATE TABLE IF NOT EXISTS Orders (OrderId INT NOT NULL, CustomerId INT NOT NULL, Amount DOUBLE, PRIMARY KEY (CustomerId, OrderId)) COLOCATE BY (CustomerID) ZONE CUSTOMER;`
5. `CREATE TABLE IF NOT EXISTS Orders_Slow ( OrderId INT NOT NULL, CustomerId INT NOT NULL, Amount Double, PRIMARY KEY (OrderId)) ZONE ORDERS;`