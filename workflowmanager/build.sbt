// Copyright (c) 2015, CodiLime Inc.

name := "deepsense-workflowmanager"

libraryDependencies ++= Dependencies.workflowmanager

unmanagedClasspath in Runtime += (baseDirectory.value / "conf")

Revolver.settings

enablePlugins(JavaAppPackaging, GitVersioning, DeepsenseUniversalSettingsPlugin)

dockerBaseImage := "anapsix/alpine-java:jre8"
dockerExposedPorts := Seq(9080)
