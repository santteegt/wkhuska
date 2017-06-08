# Watiqay Khuska - Ecuadorian Semantic Repository  #

Ecuadorian Semantic Repository of Ecuadorian Academics that allows the discovery of common research areas. The platform is based on the Apache Marmotta Linked Data Platform.

# ****IMPORTANT NOTICE**** #

## This repository is no longer under maintenance. Project has been renamed as REDI. If you're interested in the latest work being done by current maintainers, jump to this [repository](https://github.com/ucuenca/redi)

**Build status**

[ ![Codeship Status for santteegt/wkhuska](https://codeship.com/projects/26b04380-b7fa-0133-2ed7-6ef29f71ac4a/status?branch=master)](https://codeship.com/projects/134976)

### What is this repository for? ###

**Quick summary**

Watiqay Khuska is a quechua phrase that means "Research together". 

**Version**


**Sponsors and Contributors**

[CEDIA](https://www.cedia.org.ec)

Computer Science Department - University of Cuenca

### How do I get set up? ###

**System requirements**

1. Java JRE 1.7.0_45 or superior
2. Maven 3 or superior
3. Nodejs (for the npm package manager)
3. 1GB main memory
4. Java Application Server (Tomcat 7.x or Jetty 6.x)
5. Database engine(PostgreSQL, MySQL - if not explicitly configured, an embedded H2 database will be used)

**Summary of set up**

Download, fork or clone the repository from the [Repo URL](https://santteegt@bitbucket.org/ecsemanticrep/watiqay-khuska-ecuadorian-semantic-repository-for-researchers.git)

**Deployment instructions**

* Open a Terminal window and Go to the repository folder ***${wkhuska.home}***
* Execute ** mvn package install* (If you need a fast deplyment you can use the parameters ***-DskipTests=true -Dpmd.skip=true***)
* The deployed application will be installed In the directory *${wkhuska.home}/webapp/target/marmotta.war*
* If you need a faster deployment (e.g. testing purposes) you can go to the directory *${wkhuska.home}/webapp* and execute the following command from a terminal window ***mvn tomcat7:run***

**Marmotta Configuration**

If you're deploying an instance using your own Application Server you need to set the Marmotta Home Directory. (e.g. in Tomcat 7 you can set it on the setenv.sh configuration file as 
    
    *export MARMOTTA_HOME=/path/to/marmotta/home*

**Database configuration**

Marmotta comes configured with an H2 Embedded Database instance, but for production it is recommended that you configure a Database Engine. Before using the application instance you need to create both a Database Schema and a User on your preferred database Vendor, and configure it in Marmotta through the Admin interface.


### Contribution guidelines ###

**Develop a new module**

To create a new module you can make a copy of the example module *base-module* or you can create a new project using the default Marmotta maven artifacts: 

    mvn archetype:generate -DarchetypeGroupId=org.apache.marmotta -DarchetypeArtifactId=marmotta-archetype-module 

We have defined some rules to the development cycle of new platform modules:

You **MUST** follow the same pom schema as the base-module.

You **MUST** follow the Java code Conventions when programming a new module.

You **MUST** develop test cases for your module.

Your developed classes **MUST** pass the  PMD rules defined for the platform development.

**Develop a web module or interface**

Any new frontend or web interface must be deployed using the **webapp** project on the repository. You have a base frontend example on *src/main/webapp/wkhome* folder that you can clone to develop your own custom fronted app. The base technology stack that you should use for developing and testing is:

* Nodejs
* Bower
* AngularJS
* Bootstrap
* jQuery
* Karma (unit tests)
* Protractor (End to End tests)

We have defined some rules to the development cycle of new web platform modules:

The frontend libraries are defined in the *src/main/webapp/bower.json*. If you need to use new libraries, you can extend its definition using the bower package manager.

You **MUST** follow the Javascript code Conventions when programming a new web module.

You **MUST** develop unit tests and end to end tests for each feature you define in the frontend. The tests are built using Jasmine framework. You can find more information [here](http://jasmine.github.io/)

**Code review**

You can fork the repository to develop you own custom solutions or to improve the existing ones. Then, you can send us a pull request. The admin will review the code and will aprove it if it accomplish the established rules for the development cycle.

**Other guidelines**

We are improving our software development cycle. Nowadays, some developers of the department are implementing the manifestos defined for Agile Development and Continuous Delivery. We are currently working with Codeship to guarantee the continuous integration of the platform. In the future we will also offer the Continuous Deployment of our solution.

### Who do I talk to? ###

* Repo owner or admin
