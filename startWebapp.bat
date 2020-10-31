set _JAVA_OPTIONS=-Xmx16g
set secrets="secret"
set supermaas="http://localhost:8000/api/v1"
sbt "webapp/run 9001" >> webapp.out
