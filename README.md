# ConceptAlignment

Library under development for the DARPA WorldModelers program.

This library is intended to be used to align concepts from a top-down
program ontology to bottom-up concepts/indicators/variables/model components.

The REST API is served from http://linking.cs.arizona.edu which provides
access to the OpenAPI documentation, either [statically](http://linking.cs.arizona.edu/assets/openapi/webapp.yaml)
or [dynamically](http://linking.cs.arizona.edu/api).

## Notes for the initiated

* It will take any words, but preferably those found in glove.  Others don't do much good.
  
* It presently works independently of ontology.  One could enter the description or
examples associated with an ontology node like "livestock feed hay CSB silage corn meal
soybean hulls" and whatever results from the datamart scraping would be "aligned" with
wm / concept / causal_factor / interventions / provide / agriculture_inputs / livestock_production.
It would require minimal change to match against existing ontology nodes along with the
bottom-up components.
  
* It only scrapes the ISI datamarts.  NYU is not presently included.  It also does SuperMaaS models.

## Preparations

Some of the instructions below use 0.1.0 as a version number.  Change this number as necessary.
The standard port 9000 may already be used for SuperMaaS, so this project has been changed to use 9001
and that's the reason some port numbers are included in the instructions.

### Preparing the index files

This instructions are for use on a development machine.  However, once the docker
container has started up, these commands can also be run there.  Initial versions of all the
indexes will be available in the container, so these commands would be used for updating.

* Run the scrapers using the ScraperApp, which is configured for the
  * IsiScraper and
  * SuperMaasScraper.
* Run the indexers.
  * HnswlibIndexerApp is used for the KNN searches.  It should index the data from
    * ISI
    * SuperMaaS
    * Glove
  * LuceneIndexerApp creates an index to map datamartId, datasetId, and variableId to the
    * variable names and
    * variable descriptions.
* For the alignment run either the
  * SingleKnnApp directly or access it via the
  * webapp subproject.

```bash
$ # Scrape the datamarts, all of them if necessary.  Credentials are required.
$ sbt "scraper/runMain org.clulab.alignment.scraper.ScraperApp ../datamarts.tsv"
# $ For testing, sometimes SuperMaaS is only needed.
$ sbt "scraper/runMain org.clulab.alignment.scraper.SuperMaasScraperApp ../datamarts.tsv"
$ # Run this one just once because it takes a long time and glove shouldn't change.
$ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibGloveIndexerApp"
$ # Run these each time the datamarts have changed.
$ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibDatamartIndexerApp ../datamarts.tsv"
$ sbt "indexer/runMain org.clulab.alignment.indexer.lucene.LuceneIndexerApp ../datamarts.tsv"
$ # Start the server in development mode.
$ sbt webapp/run
```

The scraper for SuperMaaS is configured in `scraper/src/main/resources/application.conf`
to use `localhost:8000`.  If both ConceptAlignment and SuperMaaS are running in Docker
containers, it may be necessary to reconfigure the scraper to use something like
 `supermaas_server_1:8000`.

### Preparing the Docker image

If the webapp and other functionality is not to run on the development machine, but somewhere
else via Docker, create the image with instructions like these:

```bash
$ # Copy the index files to the docker directory so they can be accessed by the docker command.
$ cp ../hnswlib-datamart.idx ../hnswlib-glove.idx Docker
$ cp -r ../lucene-datamart Docker
$ # Create the docker image, setting the version number from version.sbt.
$ docker build -f DockerfileRun . -t clulab/conceptalignment:0.1.0
$ # If necessary, send the image to Docker Hub.
$ docker login
$ docker push clulab/conceptalignment:0.1.0
```

### Preparing the Docker container

If you are on that machine somewhere else (like perhaps you are Galois) and just need to run
the webapp, you can go about it like this:

```bash
$ # Download the image from Docker Hub if necessary.
$ docker pull clulab/conceptalignment:0.1.0
$ # Create the container which runs bash right away with the default network _or_
$ docker run -it -p 9001:9001 --name conceptalignment clulab/conceptalignment:0.1.0
$ # in order to connect to SuperMaaS locally, it will be necessary to connect to its network.
$ docker run -it -p 9001:9001 --name conceptalignment --network supermaas_supermaas clulab/conceptalignment:0.1.0
$ # Start the webapp.
$ sbt webapp/run 9001
$ # Access the webapp in a browser at http://localhost:9001.
```
