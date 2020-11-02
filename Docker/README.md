# ConceptAlignment Docker

This directory contains a Dockerfile which generates containers that run the ConceptAlignment web application
which provides the REST interface.

* DockerfileStage - This runs sbt to stage the webapp.

If you have copied the index files to this directory following instructions in the main README.md file,
you can build the image with:

```
docker build -t conceptalignment --build-arg secret=<secret> -f <Dockerfile> .
```

You can run the corresponding container with:

```
docker run -p 9001:9001 -e secret="password1|password2" -e supermaas="http://localhost:8000" conceptalignment
```

This launches the container and exposes port 9001. You can then eventually navigate to
`localhost:9001` to access the web application.

To see what is inside the image or to run things manually, try

```
docker run -it -p 9001:9001 -e secret="password1|password2" -e supermaas="http://localhost:8000" conceptalignment /bin/bash

```