RT_JAR=/usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar
INCLUDES_DIR=../
BUKKIT_TARGET=bukkit-1.7.9B1938.jar
DST_DIR=../Server/plugins

javac -Xlint:all -bootclasspath "$RT_JAR:$INCLUDES_DIR/$BUKKIT_TARGET" -d ./ src/*
jar -cfe $DST_DIR/Jail.jar net/simpvp/Jail/Jail ./*
rm -rf net/
