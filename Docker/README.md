# ConceptAlignment Docker

This directory contains a pair of Dockerfiles which generate containers that run the ConceptAlignment web application
which provides the REST interface.

* DockerfileStage - This runs `sbt webapp/stage` in the image as it is being built in order to insert files into the image.
* DockerfileStageless - This one expects `sbt` to have already run on the development machine so that the staged files can simply be copied.

If you have copied the index files to this directory following instructions in the main README.md file,
you can build the image with:

```
docker build -t conceptalignment --build-arg secret=<secret> -f <Dockerfile> . #
```

You can run the corresponding container with:

```
docker run -it -p 9001:9001 -e secrets="password1|password2" -e supermaas="http://localhost:8000/api/v1" conceptalignment
```

This launches the container and exposes port 9001. You can then eventually direct a web browser to
`localhost:9001` to access the web application.

To see what is inside the image or to run things manually, try

```
docker run -it -p 9001:9001 -e secrets="password1|password2" -e supermaas="http://localhost:8000/api/v1" conceptalignment /bin/bash

```
and then start the webapp with
```
./bin/webapp -Dhttp.port=9001
```
