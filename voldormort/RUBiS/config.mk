##############################
#    Environment variables   #
##############################

JAVA_HOME = /usr/lib/jvm/java-6-openjdk
J2EE_HOME = /home/abhi/glassfish3

JAVA  = $(JAVA_HOME)/bin/java
JAVAC = $(JAVA_HOME)/bin/javac
#JAVAC = /usr/bin/jikes
JAVACOPTS =
# +E -deprecation
JAVACC = $(JAVAC) $(JAVACOPTS)
RMIC = $(JAVA_HOME)/bin/rmic
RMIREGISTRY= $(JAVA_HOME)/bin/rmiregistry
CLASSPATH = .:$(J2EE_HOME)/lib/javaee.jar:$(JAVA_HOME)/jre/lib/rt.jar:/home/abhi/jakarta-tomcat-4.1.27/common/lib/servlet.jar:/home/abhi/Downloads/mysql-connector-java-5.0.8/mysql-connector-java-5.0.8-bin.jar:/home/abhi/voldormort/RUBiS/voldemort-0.90.1.jar:/home/abhi/RUBiS/transaction-api-1.1.jar:$(PWD)
JAVADOC = $(JAVA_HOME)/bin/javadoc
JAR = $(JAVA_HOME)/bin/jar

GENIC = ${JONAS_ROOT}/bin/unix/GenIC

MAKE = make
CP = /bin/cp
RM = /bin/rm
MKDIR = /bin/mkdir


# EJB server: supported values are jonas or jboss
EJB_SERVER = jonas

# DB server: supported values are MySQL or PostgreSQL
DB_SERVER = MySQL

%.class: %.java
	${JAVACC} -classpath ${CLASSPATH} $<

