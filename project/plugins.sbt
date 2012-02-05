resolvers += "Maven Central" at "http://repo1.maven.org/maven2"

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-proguard-plugin" % (v+"-0.1.1"))
