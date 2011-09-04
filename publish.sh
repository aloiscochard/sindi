#!/bin/sh
sbt test
echo
read -p "Press any key to start publishing..."
echo
sbt 'project sindi-compiler' +clean-files +clean +proguard +publish
rename -f s/.min.jar/.jar/ sindi-compiler/target/scala*/sindi-compiler*.min.jar
sbt +publish
sbt 'project sindi-core' +clean-files +clean +publish
