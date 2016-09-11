#!/bin/bash

sudo unzip jce_policy-8.zip
sudo cp UnlimitedJCEPolicyJDK8/local_policy.jar $JAVA_HOME/jre/lib/security
sudo cp UnlimitedJCEPolicyJDK8/README.txt $JAVA_HOME/jre/lib/security
sudo cp UnlimitedJCEPolicyJDK8/US_export_policy.jar $JAVA_HOME/jre/lib/security
sudo rm -rf UnlimitedJCEPolicyJDK8
