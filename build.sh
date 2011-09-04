#!/bin/sh
sbt 'project sindi-compiler' +clean-files +clean +proguard +publish-local
rename -f s/.min.jar/.jar/ sindi-compiler/target/scala*/sindi-compiler*.min.jar
sbt +publish-local
sbt 'project sindi-core' +clean-files +clean +publish-local
