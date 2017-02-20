ssh {
    host = "121.40.23.79"
    username = "root"
    password = "f5261458"
}

postgresql {
    conf {
        dataDirectory = "/data/test"
        listenAddresses = "localhost"
        maxConnections = 100
    }

    hba {
        trust("local", "all", "postgres", "127.0.0.1/32")
    }
}

openjdk {
    _version = "1.8"
    jdk = false
}

nginx {

}