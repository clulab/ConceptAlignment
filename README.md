[![Build Status](https://github.com/clulab/ConceptAlignment/workflows/ConceptAlignment%20CI/badge.svg)](https://github.com/clulab/ConceptAlignment/actions)
[![Maven Central](https://img.shields.io/maven-central/v/org.clulab/conceptalignment-core_2.12?logo=apachemaven)](https://mvnrepository.com/artifact/org.clulab/conceptalignment-core)
[![Docker Version](https://shields.io/docker/v/clulab/conceptalignment?sort=semver&label=docker&logo=docker)](https://hub.docker.com/r/clulab/conceptalignment/tags)


# ConceptAlignment

This library under development for the DARPA WorldModelers program.

It is intended to be used to align concepts from a top-down
program ontology to bottom-up concepts/indicators/variables/model components.

The REST API is served from https://linking.clulab.org (and http://linking.clulab.org as well as a deprecated
http://linking.cs.arizona.edu) which provides access to the OpenAPI documentation, either
[statically](https://linking.clulab.org/assets/openapi/webapp.yaml) or [dynamically](https://linking.clulab.org/api).

## Notes for the initiated

* The search function will accept any words, but preferably those found in glove.  Others don't do much good.
  
* The plain search function works independently of ontology.  (In the meantime there is a separate, specific compositionalSearch that works with that ontology.)  One could enter the description or
examples associated with an ontology node like "livestock feed hay CSB silage corn meal
soybean hulls" and whatever results from the datamart scraping would be "aligned" with
wm / concept / causal_factor / interventions / provide / agriculture_inputs / livestock_production.
It would require minimal change to match against existing ontology nodes along with the
bottom-up components.
  
* The project is presently configured to scrape DOJO indicators.  It can be made to scrape DOJO Models, ISI datamarts, and SuperMaaS models and data cubes.  NYU is not presently included.

## Subprojects

* builder - provides the initial, automatic scrape and indexing
* comparer - runs search on sample data so that performance can be compared to a different tool
* evaluator - provides a space for implementing different alignment algorithms
* experiment - provides a space for running experiments on the different alignment algorithms
* indexer - indexes scraped data for faster lookup
* scraper - scrapes data from various datamarts
* src (core) - contains library code used by other subprojects
* webapp - provides a REST interface

## Preparations

Some of the instructions below use 1.2.0 as a version number.  Change this number as necessary.
The standard port 9000 may already be used for SuperMaaS, so some instructions use 9001 instead.

Scraping the datamarts in order to index them does require authorization.  Account login information is stored in files that are specified in `./scraper/src/main/resources/application.conf`.  For obvious reasons, the files are not included in this repo.  If you need access, please ask for them.  They are formatted as such:
```properties
username = <username>
password = <password>
```

In addition, scraping via the webapp as part of its reindexing operation is also protected by a secret.  To use the functionality, record a secret value in an environment variable called `secrets` before starting the webapp and then use the same value when requesting to reindex.



### Preparing the index files

Until recently, the index files were prepared manually.  The process has now been automated.  For the time being, both sets of instructions are provided.

#### Automatic preparation

Run `BuilderApp` in the `builder` subproject. One way to do this is with the command `sbt "builder/runMain org.clulab.alignment.builder.BuilderApp"`. 

#### Manual preparation

This instructions are for use on a development machine.  However, once the docker
container has started up, these commands can also be run there.  Initial versions of all the
indexes will be available in the container, so these commands would be used for updating.

* Run the scrapers using the ScraperApp, which is configured for the
  * DojoScraper,
  * ~~IsiScraper, and~~
  * ~~SuperMaasScraper.~~
* You will need to have login credentials in order to do this.
* Run the indexers.
  * HnswlibIndexerApp is used for the KNN searches.  It should index the data from
    * DOJO
    * ~~ISI~~
    * ~~SuperMaaS~~
    * Glove
    * Flat and compositional ontologies
  * LuceneIndexerApp creates an index to map datamartId, datasetId, and variableId to the
    * variable names and
    * variable descriptions.
* For the alignment run either the
  * SingleKnnApp directly or access it via the
  * webapp subproject.

```bash
$ # Make a directory to contain the indexes of the form ../index_# where the number might be 0.
$ mkdir ../index_0
$ # Scrape the datamarts, all of them if necessary.  Credentials are required.
$ sbt "scraper/runMain org.clulab.alignment.scraper.ScraperApp ../index_0/datamarts.tsv"
$ # For testing, sometimes SuperMaaS is only needed.
$ sbt "scraper/runMain org.clulab.alignment.scraper.SuperMaasScraperApp ../index_0/datamarts.tsv"
$ # Run this one just once because it takes a long time and glove shouldn't change.  It doesn't go into ../Index_0.
$ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibGloveIndexerApp ../hnswlib-glove.idx"
$ # Run these each time the datamarts have changed.
$ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibDatamartIndexerApp ../index_0/datamarts.tsv ../index_0/hnswlib-datamart.idx"
$ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibFlatOntologyIndexerApp ../hnswlib-wm_flattened.idx"
$ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibCompositionalOntologyIndexerApp ../hnswlib-concept.idx ../hnswlib-process.idx ../hnswlib-property.idx"
$ sbt "indexer/runMain org.clulab.alignment.indexer.lucene.LuceneIndexerApp ../index_0/datamarts.tsv ../index_0/lucene-datamart"
$ # Start the server in development mode.  It should by default access ../hnswlib-glove.idx and ../index_#.
$ sbt webapp/run
```

The scraper for SuperMaaS is configured in `scraper/src/main/resources/application.conf`
to use either `localhost:8000`, or when available, whatever is recorded in the `supermaas`
environment variable.  If both ConceptAlignment and SuperMaaS are running in Docker
containers, it may be necessary to reconfigure the scraper to use something like
`supermaas_server_1:8000`.

The scraper for Dojo is configured in the same `scraper/src/main/resources/application.conf` file.  It will either use the value recorded there or, when evailable, the value in environment variable `dojo`.  

The `webapp` has the ability to download new ontologies which are supplied by a service with a REST interface.  The URL of the service is configured in `webapp/conf/application.conf` and can be overridden with the environment variable `REST_CONSUMER_ONTOLOGYSERVICE`.

### Preparing the Docker image

#### Automatic preparation

Run either `sbt dockerize` to include steps like testing and scraping and indexing or `sbt webapp/docker:publishLocal` if you know it's ready to go.  This automatic preparation does not include the credentials files, so those will need to be attached using a volume.

#### Manual preparation

If the webapp and other functionality is not to run on the development machine, but somewhere
else via Docker, create the image with instructions like these:

```bash
$ # Copy the index files to the Docker directory so they can be accessed by the `docker` command.
$ cp ../hnswlib-glove.idx Docker
$ cp ../hnswlib-wm_flattened.idx Docker
$ cp ../hnswlib-concept.idx Docker
$ cp ../hnswlib-process.idx Docker
$ cp ../hnswlib-property.idx Docker
$ cp -r ../index_0 Docker
$ # If this is available, possibly from an automatic build...
$ cp -r ../index_1 Docker
$ cp -r ../credentials Docker
```


Two docker files are provided to make the image.  For `DockerfileStage` use commands
```bash
$ # Create the docker image, setting the version number from version.sbt.
$ docker build -t clulab/conceptalignment:1.2.0 -f DockerfileStage .
```

`DockerfileStageless` needs a couple of additional files, so use these commands:
```bash
$ sbt webapp/stage
$ mv webapp/target/universal/stage Docker
$ cd Docker
$ docker build -t clulab/conceptalignment:1.2.0 -f DockerfileStageless .
```

To deploy,

```bash
$ # If necessary, send the image to Docker Hub.
$ docker login --username=<username>
$ docker push clulab/conceptalignment:1.2.0
```

### Preparing the Docker container

If you are on that machine somewhere else (like perhaps you are Galois) and just need to run
the webapp, you can go about it like this:

```bash
$ # Download the image from Docker Hub if necessary.
$ docker pull clulab/conceptalignment:1.2.0
$ # Run the webapp.
$ # If SuperMaaS is not available for indexing, skip its environment variable.
$ docker run -p 9001:9001 --name conceptalignment -e secret="<secret_for_web_server>" -e secrets="password1|password2" clulab/conceptalignment:1.2.0
$ # Include it otherwise.
$ docker run -p 9001:9001 --name conceptalignment -e secret="<secret_for_web_server>" -e secrets="password1|password2" -e supermaas="http://localhost:8000/api/v1" clulab/conceptalignment:1.2.0
$ # In order to connect to SuperMaaS running in local Docker containers, it will be necessary to connect to their Docker network.
$ docker run -p 9001:9001 --name conceptalignment --network supermaas_supermaas -e secrets="password1|password2" -e supermaas="http://localhost:8000/api/v1" clulab/conceptalignment:0.1.0
$ # Access the webapp in a browser at http://localhost:9001.
$ # If the credentials files were not included in the image, they must be added.  For automatically generated images for which they are not included, they should end up in `/conceptalignment/credentials`.
$ docker run -p 9001:9001 --name conceptalignment -e secret="<secret_for_web_server>" -e secrets="password1|password2" -v`pwd`/../credentials:/conceptalignment/credentials clulab/conceptalignment:1.2.0
$ # If server URLs need to be overridden, additional environment variables can be provided.
$ docker run -p 9001:9001 --name conceptalignment -e dojo=<URL> -e REST_CONSUMER_ONTOLOGYSERVICE=<URL> -e secret="<secret_for_web_server>" -e secrets="password1|password2" -v`pwd`/../credentials:/conceptalignment/credentials clulab/conceptalignment:1.2.0
```
