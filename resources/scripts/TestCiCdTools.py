import unittest2
import cicd_tools
import copy
import imp
import os
import sys


class TestCiCdTools(unittest2.TestCase):
    _original_argv_ = ['cicd_tools.py']

    xml = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.fedex.sefs.core.java.rest</groupId>
	<artifactId>itinerary-definition-service</artifactId>
	<version>2.0.0-SNAPSHOT</version>
	<packaging>jar</packaging>

	<name>itinerary-definition-service</name>
	<parent>
		<groupId>com.fedex.sefs.common</groupId>
		<artifactId>spring-boot-bom-parent</artifactId>
		<version>2.0.1.1</version>
	</parent>

	<dependencies>

		<!-- SEFS Itinerary Common Jar -->
		<dependency>
			<groupId>com.fedex.sefs.core.common</groupId>
			<artifactId>itinerary-common</artifactId>
			<version>2.0.4</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>

			<plugin>
					<groupId>org.jacoco</groupId>
					<artifactId>jacoco-maven-plugin</artifactId>
					<version>0.7.7.201606060606</version>
					<configuration>
						<excludes>
							<exclude>**/lib/*</exclude>
							<exclude>**/com/fedex/sefs/core/rest/itinerarydefinitionservice/configurations/*</exclude>
							<exclude>**/com/fedex/sefs/core/rest/itinerarydefinitionservice/models/*</exclude>
							<exclude>**/com/fedex/sefs/core/rest/itinerarydefinitionservice/exceptionhandler/*</exclude>
						</excludes>
					</configuration>
					<executions>
						<execution>
							<id>default-prepare-agent</id>
							<goals>
								<goal>prepare-agent</goal>
							</goals>
						</execution>
						<execution>
							<id>default-report</id>
							<phase>test</phase>
							<goals>
								<goal>report</goal>
							</goals>
							<configuration>
								<datafile>target/jacoco.exec</datafile>
								<outputDirectory>${project.build.directory}/site/jacoco-ut</outputDirectory>
							</configuration>
						</execution>
					</executions>
				</plugin>

				</plugins>
			</build>
</project>
'''
    def setUp(self):
        with  open('pom.test.xml','w') as myfile:
            myfile.write(self.xml)
        if len(self._original_argv_) < 1:
            self._original_argv_ = copy.deepcopy(sys.argv)
        sys.argv = copy.deepcopy(TestCiCdTools._original_argv_)
    def tearDown(self):
        if os.path.exists("pom.test.xml"):
            os.remove("pom.test.xml")

    def test_pom_exclusion(self):
        self.assertEquals("**/lib/*,**/com/fedex/sefs/core/rest/itinerarydefinitionservice/configurations/*,**/com/fedex/sefs/core/rest/itinerarydefinitionservice/models/*,**/com/fedex/sefs/core/rest/itinerarydefinitionservice/exceptionhandler/*",
            cicd_tools.parse_jacoco_exclusion(self.xml))

    def test_command_line(self):
        arr=['-f', 'pom.test.xml']
        for c in arr:
            sys.argv.append(c)
        runpy = imp.load_source('__main__', 'cicd_tools.py')
