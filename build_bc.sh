#!/bin/bash

for (( c=1; ;c++ ))
do
  result=$(grep -F "security.provider.$c=" "$JAVA_HOME/jre/lib/security/java.security")
  if [ "$result" ]
    then
      continue
  fi
  echo "security.provider.$c=org.bouncycastle.jce.provider.BouncyCastleProvider" >> "$JAVA_HOME/jre/lib/security/java.security"
  break
done

$(wget -P "$JAVA_HOME/jre/lib/ext/" http://www.bouncycastle.org/download/bcprov-jdk15on-155.jar)
