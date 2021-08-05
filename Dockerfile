FROM adoptopenjdk/openjdk16
#FROM zenika/kotlin
#RUN apt-get update
#RUN rm /bin/sh && ln -s /bin/bash /bin/sh
#RUN apt-get -qq -y install curl
#RUN apt-get install -y unzip
#RUN apt-get install -y zip
#RUN curl -s https://get.sdkman.io | bash
#RUN chmod a+x "$HOME/.sdkman/bin/sdkman-init.sh"
#RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install kotlin

COPY codecad-core-1.0-SNAPSHOT.jar codecad-core-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","codecad-core-1.0-SNAPSHOT.jar"]

