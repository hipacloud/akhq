### GraalVM Setup

* Install GraalVM Community 21.2.0

* Set JAVA_HOME to GraalVM
You can list available java installs by
```
java_home -V
```
```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-21.2.0/Contents/Home
```

* Add GraalVM bin to PATH
```
export PATH=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-21.2.0/Contents/Home/bin:$PATH
```

* Create virtualenv
```
graalpython -m venv venv
```

* Install PyMongo in virtualenv
```
source venv/bin/activate
pip install pymongo
```

* Start AKHQ with GraalVM
```
VENV_HOME=`pwd`/venv ./gradlew run
```
