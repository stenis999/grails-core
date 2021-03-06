/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.gradle.plugin.core

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/**
 * Gradle plugin for adding separate src/integration-test folder to hold integration tests
 *
 * Adds integrationTestCompile and integrationTestRuntime configurations that extend from testCompile and testRuntime
 *
 *
 */
class IntegrationTestGradlePlugin implements Plugin<Project> {
    boolean ideaIntegration = true
    String sourceFolderName = "src/integration-test"


    @Override
    void apply(Project project) {
        def sourceDirs = findIntegrationTestSources(project)
        if(sourceDirs) {
            def acceptedSourceDirs = []
            project.with {
                sourceSets {
                    integrationTest { sourceSet ->
                        sourceDirs.each { File srcDir ->
                            if (sourceSet.hasProperty(srcDir.name)) {
                                sourceSet."${srcDir.name}".srcDir srcDir
                                acceptedSourceDirs << srcDir
                            }
                        }
                    }
                }

                dependencies {
                    integrationTestCompile sourceSets.main.output
                    integrationTestCompile sourceSets.test.output
                    integrationTestCompile configurations.testCompile
                    integrationTestRuntime configurations.testRuntime
                }

                task(type: Test, 'integrationTest') {
                    testClassesDir = sourceSets.integrationTest.output.classesDir
                    classpath = sourceSets.integrationTest.runtimeClasspath
                    maxParallelForks = 1
                }.shouldRunAfter test

                check.dependsOn integrationTest

                if(ideaIntegration) {
                    project.afterEvaluate {
                        if(project.convention.findByName('idea')) {
                            // IDE integration for IDEA. Eclipse plugin already handles all source folders.
                            idea {
                                module {
                                    acceptedSourceDirs.each {
                                        testSourceDirs += it
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @CompileStatic
    File[] findIntegrationTestSources(Project project) {
        project.file(sourceFolderName).listFiles({File file-> file.isDirectory() && !file.name.contains('.')} as FileFilter)
    }
}
