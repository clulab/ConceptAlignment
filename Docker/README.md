# ConceptAlignment Docker

This directory contains a Dockerfile which generates containers that run the ConceptAlignment web application
which provides the REST interface.

* DockerfileRun - This runs the webapp through sbt in production mode.

If you have copied the index files to this directory following instructions in the main README.md file,
you can build the image with:

```
docker build -f <Dockerfile> . -t conceptalignment
```

You can run the corresponding container with:

```
docker run -id -p 9001:9001 --name conceptalignment conceptalignment
```

This launches the container and exposes port 9001. You can then eventually navigate to
`localhost:9001` to access the web application.
