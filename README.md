# Scala Cloudant client for Spray/Akka


## Transitive Dependencies

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

As (mentioned in the official Spray documentation)[http://spray.io/project-info/current-versions/#shapeless-versions], **if you are using Spray-routing, then you need the version built for shapeless**.
 
This library has been tested with:
```
io.spray:spray-routing-shapeless2_2.11:jar:1.3.2
```