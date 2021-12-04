#curl -X PUT 'http://localhost:8083/connectors/outbox-connector/config' \
#-H 'Content-Type: application/json' \
#-d '{
#    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
#    "plugin.name": "pgoutput",
#    "tasks.max": "1",
#    "database.hostname": "postgres",
#    "database.port": "5432",
#    "database.user": "postgres",
#    "database.password": "postgres",
#    "database.dbname" : "example-database",
#    "database.server.name": "outbox-test-postgres-server",
#    "schema.include.list": "public",
#    "table.include.list" : "public.order_outbox_event,public.warehouse_outbox_event,public.wallet_outbox_event",
#    "tombstones.on.delete" : "false",
#    "transforms" : "outbox",
#    "transforms.outbox.type" : "io.debezium.transforms.outbox.EventRouter",
#    "transforms.outbox.route.by.field" : "destination_topic",
#    "transforms.outbox.table.field.event.key":  "aggregate_id",
#    "transforms.outbox.table.field.event.payload.id": "aggregate_id",
#    "transforms.outbox.route.topic.replacement" : "${routedByValue}",
#    "transforms.outbox.table.fields.additional.placement": "type:header:eventType,trace_id:header:b3",
#    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
#    "value.converter": "org.apache.kafka.connect.storage.StringConverter"
#  }'
#{
#    "name": "inventory-connector",
#    "config": {
#        "connector.class" : "io.debezium.connector.mongodb.MongoDbConnector",
#        "tasks.max" : "1",
#        "mongodb.hosts" : "rs0/mongodb:27017",
#        "mongodb.name" : "dbserver1",
#        "mongodb.user" : "debezium",
#        "mongodb.password" : "dbz",
#        "database.whitelist" : "inventory", # 监控的 collections
#        "database.history.kafka.bootstrap.servers" : "kafka:9092"
#    }
#}
#curl --location --request POST 'http://localhost:8083/connectors' \
#--header 'Content-Type: application/json' \
#--data-raw '{
#  "name": "mongo-outbox-connector",
#  "config": {
#    "connector.class": "io.debezium.connector.mongodb.MongoDbConnector",
#    "mongodb.hosts": "rs0/mongod:27017",
#    "mongodb.name": "event"
#  }
#}'
curl --location --request POST 'http://localhost:8083/connectors' \
--header 'Content-Type: application/json' \
--data-raw '{
  "name": "mongo-connector",
  "config": {
    "connector.class" : "io.debezium.connector.mongodb.MongoDbConnector",
    "tasks.max" : "1",
    "mongodb.hosts" : "rs0/mongod:27017",
    "mongodb.name" : "event",
    "database.include.list" : "order-db,warehouse-db,wallet-db",
    "collection.include.list": "order-db.outbox-event,warehouse-db.outbox-event,wallet-db.outbox-event",
    "database.history.kafka.bootstrap.servers" : "kafka:9092",
    "transforms" : "outbox",
    "transforms.outbox.type" : "io.debezium.connector.mongodb.transforms.outbox.MongoEventRouter",
    "transforms.outbox.route.topic.replacement" : "${routedByValue}.events",
    "transforms.outbox.collection.expand.json.payload" : "true",
    "transforms.outbox.collection.field.event.timestamp" : "timestamp",
    "transforms.outbox.collection.fields.additional.placement" : "type:header:eventType",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.storage.StringConverter"
  }
}'