package com.example.demo.controller;

import com.example.demo.model.GeneratorParam;
import com.example.demo.utils.DownFileUtils;
import com.example.demo.utils.GeneratorUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.*;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class IndexController {

    public static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {

        return "index";
    }

    @RequestMapping(value = "gen", method = RequestMethod.POST)
    public void gen(HttpServletRequest req,
            HttpServletResponse res, @RequestParam String tableItems, @RequestParam String modelNames) throws IOException {

        GeneratorParam param = new GeneratorParam();

        if (!"".equals(tableItems) && !"".equals(modelNames)) {
            param.setTableNames(tableItems.split(","));
            param.setModelNames(modelNames.split(","));
        }

        List<String> warnings = new ArrayList<>();
        // 覆盖已有的重名文件
        boolean overwrite = true;
        // 准备 配置文件
        String srcDirName = UUID.randomUUID().toString().replaceAll("-", "");
        String webRoot = "";
        param.setBuildPath(webRoot + "/" + srcDirName);
        String config_path = "/mybatis-conf.xml";
        File configFile = new File(webRoot + config_path);
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

            byte[] fileByte = GeneratorUtils.zipFodler(param.getBuildPath(), webRoot + "/static/temp/" + srcDirName + ".zip");
            Executors.newFixedThreadPool(1).submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    GeneratorUtils.deleteDir(new File(webRoot + "/" + srcDirName));
                } catch (Exception e) {
                    LOGGER.error("异步删除失败", e);
                }
            });
            DownFileUtils.writeResponse(req, res, fileByte, srcDirName + ".zip");
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
        cgc.setConfigurationType("io.github.biezhi.onmybatis.utils.QnloftCommentGenerator");

        // 配置数据库属性
        JDBCConnectionConfiguration jdbcConnectionConfiguration = context.getJdbcConnectionConfiguration();

        String connection = "jdbc:mysql://" + param.getConnection() + ":" + param.getPort() + "/" + param.getDataBase();
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
        abp.setConfigurationType("io.github.biezhi.onmybatis.plugins.AddAliasToBaseColumnListPlugin");
        context.addPluginConfiguration(abp);

        PluginConfiguration pcf = new PluginConfiguration();
        pcf.setConfigurationType("io.github.biezhi.onmybatis.plugins.MapperPlugin");

        if (!"".equals(param.getMapperPlugin())) {
            pcf.addProperty("mappers", "com.kongzhong.base.BaseMapper");
        } else {
            tc.setSelectByExampleStatementEnabled(true);
            tc.setDeleteByPrimaryKeyStatementEnabled(true);
            tc.setUpdateByExampleStatementEnabled(true);
            tc.setCountByExampleStatementEnabled(true);
        }

        context.addPluginConfiguration(pcf);
        context.getTableConfigurations().clear();
        context.getTableConfigurations().add(tc);

        if ((param.getTableNames() != null) && (0 < param.getTableNames().length)) {
            //表集合
            List<TableConfiguration> tableConfigurations = context.getTableConfigurations();
            tableConfigurations.clear();
            for (int i = 0; i < param.getTableNames().length; i++) {
                if (!"".equals(param.getTableNames()[i]) && !"".equals(param.getModelNames()[i])) {
                    TableConfiguration tableConfiguration = new TableConfiguration(context);
                    tableConfiguration.setTableName(param.getTableNames()[i]);
                    tableConfiguration.setDomainObjectName(param.getModelNames()[i]);
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