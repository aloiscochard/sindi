#!/bin/sh
sbt test
echo
read -p "Press <ENTER> to start publishing..." x
echo
sbt 'project sindi-compiler' +clean-files +clean +proguard +publish-local
rename -f s/.min.jar/.jar/ sindi-compiler/target/scala*/sindi-compiler*.min.jar
sbt 'project sindi-compiler' +publish
sbt 'project sindi-core' +clean-files +clean +publish
