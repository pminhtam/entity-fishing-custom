FROM adoptopenjdk/openjdk11

USER root

RUN apt-get update

RUN apt-get install -y git
RUN apt-get install -y wget
RUN apt-get install -y software-properties-common
RUN apt-get -y --no-install-recommends install libxml2

RUN apt-get install unzip
RUN apt-get install curl 

RUN curl https://bazel.build/bazel-release.pub.gpg | apt-key add -

RUN git clone https://github.com/pminhtam/entity-fishing-custom.git \
&& cd entity-fishing-custom/dependency_install/grobid \
&& cd grobid-ner \
&& ./gradlew copyModels \
&& ./gradlew clean install \
&& cp -r resources/models/* ../grobid-home/models/ \
&& cd ../ \
&& ./gradlew clean install --no-daemon  --info --stacktrace

RUN cd entity-fishing-custom/data/db/ \
&& wget https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-kb.zip \
&& wget https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-en.zip \
&& wget https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-fr.zip \
&& wget https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-de.zip \
&& for file in *.zip; do unzip $file; rm $file; done \
&& cd ../.. \
&& ./gradlew clean assemble --no-daemon  --info --stacktrace

WORKDIR entity-fishing-custom

CMD ./gradlew appRun



