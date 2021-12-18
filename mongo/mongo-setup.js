rsconf = {
    _id : "replicasetkey123",
    members: [
        {
            "_id": 0,
            "host": "mongodb-primary:27017",
            "priority": 1
        },
        {
            "_id": 1,
            "host": "mongodb-secondary:27017",
            "priority": 0
        },
        {
            "_id": 2,
            "host": "mongodb-arbiter:27017",
            "priority": 0,
            "arbiterOnly":true
        }
    ]
}

rs.initiate(rsconf);
rs.status();
while (! db.isMaster().ismaster ) { sleep(1000) }