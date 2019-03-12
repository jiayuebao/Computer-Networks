#!/usr/bin/env bash
javac -cp org.json-chargebee-1.0.jar: Contentserver.java
java -cp org.json-chargebee-1.0.jar:. Contentserver $1 $2