set -xe
javac -cp .:choco-solver-4.0.1-with-dependencies.jar $1.java
java -cp .:choco-solver-4.0.1-with-dependencies.jar $1
