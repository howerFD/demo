package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.demo.model.GeneratorParam;
import com.example.demo.utils.DownFileUtils;
import com.example.demo.utils.GeneratorUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.CommentGeneratorConfiguration;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.config.JavaClientGeneratorConfiguration;
import org.mybatis.generator.config.JavaModelGeneratorConfiguration;
import org.mybatis.generator.config.PluginConfiguration;
import org.mybatis.generator.config.SqlMapGeneratorConfiguration;
import org.mybatis.generator.config.TableConfiguration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class IndexController {

    public static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {

        return "index";
    }

    @RequestMapping(value = "/generate", method = RequestMethod.POST)
    public void generate(@RequestBody GeneratorParam param,
            HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (!"".equals(param.getTableNamesString()) && !"".equals(param.getModelNamesString())) {

            List<String> tableNames = new ArrayList<String>();
            String[] tableNameString = param.getTableNamesString().split(",");
            for(int i = 0;  i< tableNameString.length; i++){
                tableNames.add(tableNameString[i]);
            }
            List<String> modelNames = new ArrayList<String>();
            String[] modelNameString = param.getModelNamesString().split(",");
            for(int i = 0;  i< modelNameString.length; i++){
                modelNames.add(modelNameString[i]);
            }

            param.setTableNames(tableNames);
            param.setModelNames(modelNames);
        }

        List<String> warnings = new ArrayList<>();
        // 覆盖已有的重名文件
        boolean overwrite = true;
        // 准备 配置文件
        String srcDirName = UUID.randomUUID().toString().replaceAll("-", "");
        String webRoot = IndexController.class.getResource("/static/temp").getPath();
        String config_path = "/mybatis-conf.xml";
        param.setBuildPath(webRoot + "/" + srcDirName);
        File configFile = new File(IndexController.class.getResource(config_path).getPath());
        try {
            // 1.创建 配置解析器
            ConfigurationParser parser = new ConfigurationParser(warnings);
            // 2.获取 配置信息
            Configuration config = parser.parseConfiguration(configFile);

            // 应用配置信息
            applyConfig(config, param);

            // 3.创建 默认命令解释调回器
            DefaultShellCallback callback = new DefaultShellCallback(overwrite);
            // 4.创建 mybatis的生成器
            MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
            // 5.执行，关闭生成器
            try {
                myBatisGenerator.generate(null);
            } catch (Exception e) {
                LOGGER.error("", e);
            }

            byte[] fileByte = GeneratorUtils.zipFodler(param.getBuildPath(), webRoot + "/" + srcDirName + ".zip");
            DownFileUtils.writeResponse(req, res, fileByte, "new.zip");
            Executors.newFixedThreadPool(1).submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    GeneratorUtils.deleteDir(new File(webRoot + "/" + srcDirName));
                } catch (Exception e) {
                    LOGGER.error("异步删除失败", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    void applyConfig(Configuration config, GeneratorParam param) {
        File dirFile = new File(param.getBuildPath());
        if (!dirFile.exists()) {
            dirFile.mkdirs();
            new File(param.getBuildPath() + "/src/main/java").mkdirs();
            new File(param.getBuildPath() + "/src/main/resources").mkdirs();
        }

        Context context = config.getContexts().get(0);

        // 注释
        CommentGeneratorConfiguration cgc = context.getCommentGeneratorConfiguration();
        cgc.setConfigurationType("cpm.example.demo.utils.QnloftCommentGenerator");

        // 配置数据库属性
        JDBCConnectionConfiguration jdbcConnectionConfiguration = context.getJdbcConnectionConfiguration();

        jdbcConnectionConfiguration.setDriverClass("org.postgresql.Driver");

        String connection = "jdbc:postgresql://" + param.getConnection() + ":" + param.getPort() + "/" + param.getDataBase();
        jdbcConnectionConfiguration.setConnectionURL(connection);
        jdbcConnectionConfiguration.setUserId(param.getUserId());
        jdbcConnectionConfiguration.setPassword(param.getUserPass());

        //配置模型的包名
        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = context.getJavaModelGeneratorConfiguration();
        javaModelGeneratorConfiguration.setTargetPackage(param.getModelPath());
        javaModelGeneratorConfiguration.setTargetProject(param.getBuildPath() + "/src/main/java");

        //mapper的包名
        JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = context.getJavaClientGeneratorConfiguration();
        javaClientGeneratorConfiguration.setTargetPackage(param.getMapperPath());
        javaClientGeneratorConfiguration.setTargetProject(param.getBuildPath() + "/src/main/java");

        //映射文件的包名
        SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = context.getSqlMapGeneratorConfiguration();
        sqlMapGeneratorConfiguration.setTargetPackage(param.getMappingPath());
        sqlMapGeneratorConfiguration.setTargetProject(param.getBuildPath() + "/src/main/resources");

        TableConfiguration tc = new TableConfiguration(context);
        tc.setTableName("%");
        tc.setAllColumnDelimitingEnabled(true);

        // 插件
        PluginConfiguration sp = new PluginConfiguration();
        sp.setConfigurationType("org.mybatis.generator.plugins.SerializablePlugin");
        context.addPluginConfiguration(sp);

        PluginConfiguration abp = new PluginConfiguration();
        abp.setConfigurationType("com.example.demo.plugins.AddAliasToBaseColumnListPlugin");
        context.addPluginConfiguration(abp);

        PluginConfiguration pcf = new PluginConfiguration();
        pcf.setConfigurationType("com.example.demo.plugins.MapperPlugin");

        if (!"".equals(param.getMapperPlugin())) {
            pcf.addProperty("mappers", "com.example.demo.BaseMapper");
        } else {
            tc.setSelectByExampleStatementEnabled(true);
            tc.setDeleteByPrimaryKeyStatementEnabled(true);
            tc.setUpdateByExampleStatementEnabled(true);
            tc.setCountByExampleStatementEnabled(true);
        }

        context.addPluginConfiguration(pcf);
        context.getTableConfigurations().clear();
        context.getTableConfigurations().add(tc);

        if ((param.getTableNames() != null) && (0 < param.getTableNames().size())) {
            //表集合
            List<TableConfiguration> tableConfigurations = context.getTableConfigurations();
            tableConfigurations.clear();
            for (int i = 0; i < param.getTableNames().size(); i++) {
                if (!"".equals(param.getTableNames().get(i)) && !"".equals(param.getModelNames().get(i))) {
                    TableConfiguration tableConfiguration = new TableConfiguration(context);
                    tableConfiguration.setTableName(param.getTableNames().get(i));
                    tableConfiguration.setDomainObjectName(param.getModelNames().get(i));
                    tableConfiguration.setCountByExampleStatementEnabled(true);
                    tableConfiguration.setDeleteByExampleStatementEnabled(true);
                    tableConfiguration.setSelectByExampleStatementEnabled(true);
                    tableConfiguration.setUpdateByExampleStatementEnabled(true);
                    //模型是否驼峰命名，为0则为驼峰
                    if (param.getIsHump().equals("0"))
                        tableConfiguration.getProperties().setProperty("useActualColumnNames", "true");
                    tableConfigurations.add(tableConfiguration);
                }
            }
        }
    }

}