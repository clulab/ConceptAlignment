export _JAVA_OPTIONS=-Xmx12g
export secrets="secret"
export supermaas="http://localhost:8000/api/v1"
sbt "webapp/run 9001" >> webapp.out
