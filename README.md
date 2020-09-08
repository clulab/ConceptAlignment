# ConceptAlignment

Library under development for the DARPA WorldModelers program.

This library is intended to be used to align concepts from a top-down
program ontology to bottom-up concepts/indicators/variables/model components.

The REST API is served from http://linking.cs.arizona.edu which provides
access to the OpenAPI documentation, either [statically](http://linking.cs.arizona.edu/assets/openapi/webapp.yaml)
or [dynamically](http://linking.cs.arizona.edu/api).

## Notes for the initiated

* It will take any words, preferably those found in glove.  Others don't do much good.
  
* It presently works independently of ontology.  One could enter the description or
examples associated with an ontology node like "livestock feed hay CSB silage corn meal
soybean hulls" and whatever results from the datamart scraping would be "aligned" with
wm / concept / causal_factor / interventions / provide / agriculture_inputs / livestock_production.
It would require minimal change to match against existing ontology nodes along with the
bottom-up components.
  
* It only scrapes the ISI datamarts.  NYU is not presently included.  It also does SuperMaaS models.
