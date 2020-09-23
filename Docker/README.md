# ConceptAlignment Docker

This directory contains a Dockerfile which generates containers that run the ConceptAlignment web application and web service.

* DockerfileRun - This runs the webapp through sbt in development mode..

If you have copied the index files to this directory following instructions in the main README.md file,
you can build the image with:

```
docker build -f <Dockerfile> . -t conceptalignment
```

You can run the corresponding container with:

```
docker run -id -p 9000:9000 --name conceptalignment conceptalignment
```

This launches the container and exposes port 9000. You can then run a shell in it to
perform necessary tasks and eventually navigate to `localhost:9000` to access the web application.

