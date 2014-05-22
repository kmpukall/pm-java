#! /bin/sh
ECLIPSE_HOME=~/opt/eclipse

rm -f pm_refactoring
ln -s ../src/pm_refactoring ./pm_refactoring
export CLASSPATH=`echo \`find ${ECLIPSE_HOME}/plugins -name "*.jar"\` | tr ' ' ':'`
javac -target 1.5 `find -L . -name "*.java"`
rm -f autofrob.jar
jar cf autofrob.jar `find autofrob -name "*.class"`  `find -L pm_refactoring -name "*.class" | grep -v pm_refactoring/tests`
