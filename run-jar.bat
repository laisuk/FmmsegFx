@echo off
rem java -jar "-Dfile.encoding=UTF-8" --module-path "C:\Java\javafx-sdk-22.0.2\lib" --add-modules javafx.controls,javafx.fxml build/libs/fmmsegfx-1.0-SNAPSHOT.jar
%USERPROFILE%\.jdks\graalvm-jdk-21.0.7\bin\java.exe  "-Dfile.encoding=UTF-8" --module-path "C:\Java\javafx-sdk-23.0.2\lib;C:\Java\richtextfx" --add-modules javafx.controls,javafx.fxml,org.fxmisc.richtext -jar build/libs/fmmsegfx-2.0-SNAPSHOT.jar
