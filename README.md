# Scala Cloudant client for Spray/Akka

## Motivation
Most Cloudant/CouchDB libraries I've came across are overly complicated, and require a bit of a learning curve to use. Error handling and debugging response unmarshalling error wasn't very pleasant. They do have some really nice and advanced features, but I didn't really require them. Since I'm using Spray, and Cloudant already provide a HTTP API, it was fairly straight forward to create a new client.

 **About 200 lines of Scala later, here is it.**

#### Features
- **CRUD** for Database/Document/Bulk/Index/Views and **Search** Index/View method are provided.
- Everything returns a **Future[String] by default**, this is the raw JSON response from Cloudant.
- Unmarshalling is optional, with a **.unmarshalTo[T] convenience method** provided.
- Exceptions (CloudantException) will come through **the usual Future.failed** channel.

## Usage Example

Just extend the CloudantOps trait and the cloudant operations will be available.

```scala
class FoodService extends Actor with CloudantOps {
      // Alternatively: CloudantCredential(username, accountname, password)
      implicit val cloudantCred = new CloudantCredential(username, password)
      implicit val dbName  = "myDB"
      
      // Create doc via JSON directly
      // Notice _id and _rev field is omitted, this will create a new document.
      val doc = """{"food": "Pizza"}"""
      val response1: Future[String] = createOrUpdateDoc(doc)
      
      // Create doc via object
      case class MyFood(food: String)
      val myFood = MyFood("Pizza")
      val response2: Future[String] = createOrUpdateDoc[MyFood](myFood)
      
      // Create doc via object, then unmarshall the response
      val response3: Future[MyResponseType] = createOrUpdateDoc[MyFood](myFood).unmarshalTo[MyResponseType]
}
```

The best documentation is the [code itself](https://github.com/icha024/spray-cloudant/blob/master/src/main/scala/com/clianz/cloudant/CloudantOps.scala), it'll only take 2 minutes to glance through the code.

### Client API Available
##### DB CRUD Operations
- createDb
- retrieveDb
- retrieveAllDbs
- deleteDb

##### Doc CRUD Operations
- createOrUpdateDoc
- createOrUpdateOrDeleteBulkDoc
- retrieveDoc
- deleteDoc

##### Index/View Operations
- createOrUpdateIndex
- createOrUpdateView
- searchIndex
- searchView

#### Unsupported Operations
- Retrieve all doc on a DB
- Retrieve DB changes
- Querying view by composite key 

## Installing the client
Check Maven Central for the latest published version.

[![Maven Central](https://img.shields.io/maven-central/v/com.clianz/spray-cloudant_2.11.svg)](http://search.maven.org/#search%7Cga%7C1%7Cspray-cloudant)

### Maven

```xml
<dependency>
    <groupId>com.clianz</groupId>
    <artifactId>spray-cloudant_2.11</artifactId>
    <version>0.1.3</version>
</dependency>
```

### SBT

```properties
libraryDependencies += "com.clianz" % "spray-cloudant_2.11" % "0.1.3"
```

### Transitive Dependencies

This library uses Scala 2.11, Spray/Akka, and the Shapeless library (via Spray-json-shapless).

```
com.clianz:spray-cloudant_2.11:jar:0.1-SNAPSHOT
+- org.scala-lang:scala-compiler:jar:2.11.7:compile
|  +- org.scala-lang:scala-library:jar:2.11.7:compile
|  +- org.scala-lang:scala-reflect:jar:2.11.7:compile
|  +- org.scala-lang.modules:scala-xml_2.11:jar:1.0.4:compile
|  \- org.scala-lang.modules:scala-parser-combinators_2.11:jar:1.0.4:compile
+- com.typesafe.akka:akka-actor_2.11:jar:2.4.4:compile
|  +- com.typesafe:config:jar:1.3.0:compile
|  \- org.scala-lang.modules:scala-java8-compat_2.11:jar:0.7.0:compile
+- io.spray:spray-client_2.11:jar:1.3.2:compile
|  +- io.spray:spray-can_2.11:jar:1.3.2:compile
|  |  \- io.spray:spray-io_2.11:jar:1.3.2:compile
|  +- io.spray:spray-http_2.11:jar:1.3.2:compile
|  |  \- org.parboiled:parboiled-scala_2.11:jar:1.1.6:compile
|  |     \- org.parboiled:parboiled-core:jar:1.1.6:compile
|  +- io.spray:spray-httpx_2.11:jar:1.3.2:compile
|  |  \- org.jvnet.mimepull:mimepull:jar:1.9.4:compile
|  \- io.spray:spray-util_2.11:jar:1.3.2:compile
\- com.github.fommil:spray-json-shapeless_2.11:jar:1.2.0:compile
   +- io.spray:spray-json_2.11:jar:1.3.2:compile
   +- org.slf4j:slf4j-api:jar:1.7.16:compile
   \- com.chuusai:shapeless_2.11:jar:2.3.0:compile
      \- org.typelevel:macro-compat_2.11:jar:1.1.1:compile
```

#### Spray-routing version
As [mentioned in the official Spray documentation](http://spray.io/project-info/current-versions/#shapeless-versions), **if you are using Spray-routing, then you need the version built for shapeless**.
 
This library was tested with:
```
io.spray:spray-routing-shapeless2_2.11:jar:1.3.2
```
