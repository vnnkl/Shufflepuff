#!/bin/bash

for (( c=1; ;c++ ))
do
  result=$(grep "security.provider.$c=" "$JAVA_HOME/jre/lib/security/java.security")
  if [ "$result" ]
    then
      continue
  fi
  echo "security.provider.$c=org.bouncycastle.jce.provider.BouncyCastleProvider" >> "$JAVA_HOME/jre/lib/security/java.security"
  break
done

$(curl -o "$JAVA_HOME/jre/lib/ext/bcprov-jdk15on-155.jar" http://www.bouncycastle.org/download/bcprov-jdk15on-155.jar)
