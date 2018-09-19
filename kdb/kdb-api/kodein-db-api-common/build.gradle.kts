plugins {
    id("kodein-common")
}

dependencies {
    compile(project(":ldb:ldb-api:kodein-leveldb-api-common"))
}

kodeinPublication {
    upload {
        name = "Kodein-DB-API-Common"
        description = "Kodein DB API Commons"
        repo = "Kodein-DB"
    }
}
