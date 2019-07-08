#!/bin/sh

java -cp .:mybatis-generator-core-1.3.7.jar org.mybatis.generator.api.ShellRunner -configfile generatorConfig.xml -overwrite
