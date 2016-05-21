# Scala Cloudant client for Spray/Akka

## Motivation
Most Cloudant/CouchDB libraries I've came across are overly complicated, and require a bit of a learning curve to use. The other problem I had was unmarshalling response that mismatched my expected JSON object, debugging those and doing exception handling was not a very pleasant experience. Since I'm using Spray, and Cloudant already provide a HTTP API, I've decided to create a simple Cloudant client to overcome those pain points.
 
 **About 200 lines of Scala later, here is it.**

#### Features
- **CRUD** for Database/Document/Bulk/Index/Views and **Search** Index/View method are provided.
- Everything returns a **Future[String] by default**, this is the raw JSON response from Cloudant.
- Unmarshalling is optional, with a **.unmarshalTo[T] convenience method** provided.
- Exceptions (CloudantException) will come through **the usual Future.failed** channel.

## Example

```scala
trait LocatorService extends Actor with CloudantOps {
      implicit val dbName  = "myDB"
      implicit val cloudantCred = new CloudantCredential(username, password)
      
      // Create doc via JSON directly
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

The best documentation is the code itself, take a look, it'll only take 2 minutes to glance through and figure it out.

## Transitive Dependencies

This library depends on Spray/Akka, and uses the Shapeless library.

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
As (mentioned in the official Spray documentation)[http://spray.io/project-info/current-versions/#shapeless-versions], **if you are using Spray-routing, then you need the version built for shapeless**.
 
This library was tested with:
```
io.spray:spray-routing-shapeless2_2.11:jar:1.3.2
```