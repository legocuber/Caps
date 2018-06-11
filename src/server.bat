@echo off
@javac caps/*.java
@javac caps/server/*.java
@javac caps/server/swing/*.java
@javac caps/client/*.java
@javac caps/client/swing/*.java
start "" javaw caps/server/Server