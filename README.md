#Contribly reference front end

This application is an example web front end to the Contribly User Generated Content API.

It is intended to demonstrate basic authentication, contribution and retrieval of content and the associated Contribly API calls.

This example is implemented in Scala and Play Framework 2.5.


## Usage

Requires Java JDK 8 and Scala 2.11.8

git clone https://github.com/contribly/ugc-frontend.git
cd ugc-frontend

Ammend conf/application.conf to set the ugc.user, ugc.client.id and ugc.client.secret conf keys (which will be supplied by Contribly).

sbt -Dhttp.port=9010
run

The moderated content of your Contribly instance should be visible on localhost:9010
