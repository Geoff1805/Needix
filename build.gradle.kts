// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    constraints {
      classpath("io.netty:netty-codec:4.2.19.Final")
      classpath("io.netty:netty-codec-http:4.2.19.Final")
      classpath("io.netty:netty-codec-http2:4.2.19.Final")
      classpath("io.netty:netty-handler:4.2.19.Final")
      classpath("io.netty:netty-handler-proxy:4.2.19.Final")
      classpath("io.netty:netty-buffer:4.2.19.Final")
      classpath("io.netty:netty-common:4.2.19.Final")
      classpath("io.netty:netty-transport:4.2.19.Final")
      classpath("io.netty:netty-resolver:4.2.19.Final")
      classpath("org.bouncycastle:bcprov-jdk18on:1.86")
      classpath("org.bouncycastle:bcpkix-jdk18on:1.86")
      classpath("org.bouncycastle:bcutil-jdk18on:1.86")
      classpath("org.jdom:jdom2:2.0.6.1")
      classpath("org.apache.httpcomponents:httpclient:4.5.14")
      classpath("org.apache.commons:commons-lang3:3.21.0")
      classpath("org.apache.commons:commons-compress:1.28.0")
      classpath("commons-io:commons-io:2.22.0")
      classpath("org.bitbucket.b_c:jose4j:0.9.7")
      classpath("com.google.guava:guava:33.7.2-jre")
      classpath("com.google.protobuf:protobuf-java:3.25.9")
    }
  }
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      when {
        requested.group == "io.netty" &&
          requested.name != "netty-tcnative-boringssl-static" &&
          requested.name != "netty-tcnative-classes" -> useVersion("4.1.139.Final")
        requested.group == "org.bouncycastle" &&
          requested.name in setOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on") -> useVersion("1.86")
        requested.group == "org.jdom" && requested.name == "jdom2" -> useVersion("2.0.6.1")
        requested.group == "org.apache.httpcomponents" && requested.name == "httpclient" -> useVersion("4.5.14")
        requested.group == "org.apache.commons" && requested.name == "commons-lang3" -> useVersion("3.21.0")
        requested.group == "org.apache.commons" && requested.name == "commons-compress" -> useVersion("1.28.0")
        requested.group == "commons-io" && requested.name == "commons-io" -> useVersion("2.22.0")
        requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> useVersion("0.9.7")
        requested.group == "com.google.guava" && requested.name == "guava" -> useVersion("33.7.2-jre")
        requested.group == "com.google.protobuf" &&
          requested.name in setOf("protobuf-java", "protobuf-java-util", "protobuf-kotlin") -> useVersion("3.25.9")
      }
    }
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
}
