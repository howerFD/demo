package com.example.demo.model;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneratorParam implements Serializable {

    private String driverClass;
    private String connection;
    private String dataBase;
    private String port;
    private String userId;
    private String userPass;
    private String modelPath;
    private String mappingPath;
    private String mapperPath;
    private String buildPath;
    private String tableNamesString;
    private String modelNamesString;
    private List<String> tableNames;
    private List<String> modelNames;
    private String isHump;
    private String isAlltable;
    private String mapperPlugin;
}