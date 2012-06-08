resolvers += "Maven Central" at "http://repo1.maven.org/maven2"

libraryDependencies <+= sbtVersion(v => "com.github.aloiscochard" %% "xsbt-proguard-plugin" % (v+"-0.1.2"))

addSbtPlugin("com.github.aloiscochard" %% "xsbt-fmpp-plugin" % "0.2")

