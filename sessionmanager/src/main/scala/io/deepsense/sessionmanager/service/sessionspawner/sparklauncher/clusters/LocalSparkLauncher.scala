/**
 * Copyright (c) 2016, CodiLime Inc.
 */

package io.deepsense.sessionmanager.service.sessionspawner.sparklauncher.clusters

import org.apache.spark.launcher.SparkLauncher

import io.deepsense.sessionmanager.rest.requests.ClusterDetails
import io.deepsense.sessionmanager.service.sessionspawner.SessionConfig
import io.deepsense.sessionmanager.service.sessionspawner.sparklauncher.SparkLauncherConfig
import io.deepsense.sessionmanager.service.sessionspawner.sparklauncher.executor.LocalSessionExecutorArgs

private [clusters] object LocalSparkLauncher {

  def apply(
      sessionConfig: SessionConfig,
      config: SparkLauncherConfig,
      clusterConfig: ClusterDetails): SparkLauncher = {
    new SparkLauncher()
      .setVerbose(true)
      .setMainClass(config.className)
      .setMaster("local[*]")
      .setDeployMode("client")
      .setAppResource(config.weJarPath)
      .setAppName("SessionExecutor")
      .addAppArgs(LocalSessionExecutorArgs(sessionConfig, config, clusterConfig): _*)
      .addFile(config.weDepsPath)
      .setConf("spark.executorEnv.PYTHONPATH", config.weDepsPath)
      .setConf("spark.driver.extraClassPath", config.weJarPath)
      .setConf("spark.driver.extraJavaOptions",
        "-XX:MaxPermSize=1024m -XX:PermSize=256m -Dfile.encoding=UTF8")
  }

}