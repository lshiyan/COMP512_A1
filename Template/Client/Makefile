all: compile-client

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

compile-client: ../Server/RMIInterface.jar
	javac -cp ../Server/RMIInterface.jar Client/Client.java Client/Command.java Client/RMIClient.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
