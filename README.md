# docker-komposer 
[![Build Status](https://travis-ci.org/zenvia-mobile/docker-komposer.svg?branch=master)](https://travis-ci.org/zenvia-mobile/docker-komposer)

This project aims to make easy to use docker-compose feature inside Java/Groovy projects.

It comes with an JUnit rule implementation to allow the container creation on java testing.


## How to Use:

Import the project [dependecy from maven](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.zenvia.komposer%22%20AND%20a%3A%22docker-komposer%22) on your specific build tool

Create the Rule object:

```java
    @ClassRule public static KomposerRule container = new KomposerRule("docker-compose-test.yml", "docker.properties", false); // to not execute pull, broken yet :( !!!
    container.getContainers().get("serviceName").getContainerInfo(); // returns the container inspect from docker
```

The docker.properties file is optional, and can contain:
 
```properties
host= docker host
cert.path= if is a secure connection
# in case of using docker hub to pull private images
hub.user= 
hub.pass=
hub.email=
```

If property file is informed, docker client will try to connect using environment variables.

