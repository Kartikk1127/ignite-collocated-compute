# Running Apache Ignite 3

Follow these steps to spin up a 3-node Apache Ignite 3 cluster using Docker and set up the schema.

1. Create a directory of your choice.
2. Place the docker compose file from the project in that directory.
3. Change the directory to the one you created right now.
4. Run `docker compose up -d` OR `docker-compose up -d`
5. Once the containers are running, follow the below steps:
   1. Run `docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli`
   2. Answer "Yes" to the prompt.
   3. Run `connect http://localhost:10300` -- Should show :: `Connected to http://localhost:10300`
   4. Run `cluster init --name=ignite3 --metastorage-group=node1,node2,node3` to initialize the cluster.
   5. Enter the sql terminal by executing: `sql` -- The sql terminal should have opened by now.
6. Follow the `ignite.sql` file and run all queries one by one.
7. We are now ready to execute the code.
8. Head over to Readme.md to run the Java diagnostics and benchmarking tool.