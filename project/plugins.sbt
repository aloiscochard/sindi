resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies <+= sbtVersion(v => "com.github.aloiscochard" %% "xsbt-proguard-plugin" % (v+"-0.1.2"))

addSbtPlugin("com.github.aloiscochard" %% "xsbt-fmpp-plugin" % "0.2")

