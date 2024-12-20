import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.config.JvmTarget

/*
 * Copyright 2020 Alpha Cephei Inc. & Doomsdayrs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
	kotlin("multiplatform") version "1.8.10"
	id("com.android.library")
	`maven-publish`
	id("org.jetbrains.dokka") version "1.7.20"
	kotlin("plugin.serialization") version "1.8.10"
}

group = "com.alphacephei"
version = "0.3.50"

repositories {
	google()
	mavenCentral()
}

val dokkaOutputDir = "$buildDir/dokka"

tasks.getByName<DokkaTask>("dokkaHtml") {
	outputDirectory.set(file(dokkaOutputDir))
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
	delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
	dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
	archiveClassifier.set("javadoc")
	from(dokkaOutputDir)
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.native(
	configure: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests.() -> Unit = {}
) {
	when {
		org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw -> mingwX64("native")
		org.jetbrains.kotlin.konan.target.HostManager.hostIsLinux -> linuxX64("native")
		org.jetbrains.kotlin.konan.target.HostManager.hostIsMac -> if (org.jetbrains.kotlin.konan.target.HostManager.hostArch() == "arm64") {
			macosArm64("native")
		} else {
			macosX64("native")
		}

		else -> error("Unsupported Host OS: ${org.jetbrains.kotlin.konan.target.HostManager.hostOs()}")
	}.apply(configure)
}

kotlin {
	jvm {
		compilations.all {
			kotlinOptions.jvmTarget = JvmTarget.JVM_11.description
		}
		testRuns["test"].executionTask.configure {
			useJUnitPlatform()
			environment("MODEL", "VOSK_MODEL")
			//environment("MODEL", "/home/doomsdayrs/Downloads/vosk-model-small-en-us-0.15/")
			environment("LIBRARY", "VOSK_PATH")
			//environment("LIBRARY", "/usr/local/lib64/libvosk/libvosk.so")
			environment("AUDIO", "$projectDir/../python/example/test.wav")
		}
	}

	android {
		publishAllLibraryVariants()
	}

	/**
	 * If native target should be enabled or not.
	 *
	 * Currently disabled as there is no proper packaging distribution currently.
	 */
	@Suppress("SimplifyBooleanWithConstants") // Ignore, the false is for overrides
	val enableNative = System.getenv("NATIVE_EXPERIMENT") == "true" || false

	if (enableNative)
		native {
			val main by compilations.getting
			val libvosk by main.cinterops.creating

			binaries {
				sharedLib()
			}
		}

	publishing {
		publications {
			withType<MavenPublication> {
				artifact(javadocJar)
				pom {
					url.set("http://www.alphacephei.com.com/vosk/")
					licenses {
						license {
							name.set("The Apache License, Version 2.0")
							url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
						}
					}
					developers {
						developer {
							id.set("com.alphacephei")
							name.set("Alpha Cephei Inc")
							email.set("contact@alphacephei.com")
						}
					}
					scm {
						connection.set("scm:git:git://github.com/alphacep/vosk-api.git")
						url.set("https://github.com/alphacep/vosk-api/")
					}
				}
			}
		}
	}

	val jna_version = "5.13.0"
	val coroutines_version = "1.6.4"

	sourceSets {
		val commonMain by getting {
			dependencies {
				api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
				api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
			}
		}
		val commonTest by getting {
			dependencies {
				implementation(kotlin("test"))
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")
			}
		}
		val jvmMain by getting {
			dependencies {
				api("net.java.dev.jna:jna:$jna_version")
			}
		}
		val jvmTest by getting
		if (enableNative) {
			val nativeMain by getting
		}
		val androidMain by getting {
			dependsOn(jvmMain)
			dependencies {
				api("net.java.dev.jna:jna:$jna_version@aar")
			}
		}
		val androidTest by getting {
			dependencies {
				implementation("junit:junit:4.13.2")
			}
		}
	}
}

android {
	compileSdk = 33
	sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
	defaultConfig {
		minSdk = 24
		targetSdk = 33
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	publishing {
		multipleVariants {
			withSourcesJar()
			withJavadocJar()
			allVariants()
		}
	}
}
