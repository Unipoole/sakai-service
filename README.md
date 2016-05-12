[![Build Status](https://travis-ci.org/Unipoole/sakai-service.svg?branch=master)](https://travis-ci.org/Unipoole/sakai-service)
[![License](https://img.shields.io/badge/License-ECL%202.0-blue.svg)](https://opensource.org/licenses/ECL-2.0)
# sakai-service
The Sakai-Service contains the web service implementations (build to Axis jws files) that must be deployed to Sakai.
This is a library project and it does contain deployable artefacts, but the artefacts must be deployed into a Axis Web Context.

## Building
```bash
git clone https://github.com/Unipoole/sakai-service.git
cd sakai-service
mvn clean install
```

## Deployment
If the Maven build is successful there will be a `sakai-service-1.0.0-jws.zip` archive in the target folder of the build. This archive must be extracted to the Sakai Axis project. In the Sakai source it can be found under `webservices\axis\src\webapp`. In a existing deployment it can be found under `<tomcat>\webapps\sakai-axis`.