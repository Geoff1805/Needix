pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

buildscript {
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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Needix"

include(":app")

gradle.beforeProject {
  buildscript.configurations.configureEach {
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
