package com.generator.utils;

import com.generator.entity.ColumnEntity;
import com.generator.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 * @author
 */
public class GenUtils {

    private static Map<String, String> getAttachTemplates() {
        Map<String, String> templates = new HashMap<>(7);

        templates.put("templates/attach/application-dev.yml.vm", "application-dev.yml");
        templates.put("templates/attach/application-prod.yml.vm", "application-prod.yml");
        templates.put("templates/attach/application-test.yml.vm", "application-test.yml");
        templates.put("templates/attach/BootApplication.java.vm", "BootApplication.java");
        //templates.put("templates/attach/DefaultHttpRequestAspect.java.vm", "DefaultHttpRequestAspect.java");
        templates.put("templates/attach/FilterConfiguration.java.vm", "FilterConfiguration.java");
        templates.put("templates/attach/ServerProperties.java.vm", "ServerProperties.java");
        templates.put("templates/attach/SwaggerConfiguration.java.vm", "SwaggerConfiguration.java");
        templates.put("templates/attach/client.pom.xml.vm", "pom.xml");
        templates.put("templates/attach/master.pom.xml.vm", "pom.xml");
        templates.put("templates/attach/service.pom.xml.vm", "pom.xml");
        templates.put("templates/attach/logback.xml.vm", "logback.xml");


        templates.put("templates/attach/BaseControllerImpl.java.vm", "BaseControllerImpl.java");
        templates.put("templates/attach/BaseDAO.java.vm", "BaseDAO.java");
        templates.put("templates/attach/BaseDO.java.vm",  "BaseDO.java");
        templates.put("templates/attach/BaseQueryDO.java.vm", "BaseQueryDO.java");
        templates.put("templates/attach/BaseServiceAO.java.vm", "BaseServiceAO.java");
        templates.put("templates/attach/BaseServiceAOImpl.java.vm", "BaseServiceAOImpl.java");
        templates.put("templates/attach/CommonCode.java.vm", "CommonCode.java");
        templates.put("templates/attach/CommonResult.java.vm", "CommonResult.java");
        templates.put("templates/attach/DateUtil.java.vm", "DateUtil.java");
        templates.put("templates/attach/DistribID.java.vm", "DistribID.java");
        templates.put("templates/attach/EnvironmentDefine.java.vm", "EnvironmentDefine.java");

        return templates;
    }

    private static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("templates/DO.java.vm");
        templates.add("templates/Query.java.vm");
        templates.add("templates/Dao.java.vm");
        templates.add("templates/Dao.xml.vm");
        templates.add("templates/Service.java.vm");
        templates.add("templates/ServiceImpl.java.vm");
        templates.add("templates/Controller.java.vm");
        templates.add("templates/Api.java.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorAttachCode(ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;


        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String projectPath = config.getString("projectPath");
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "com.generator" : mainPath;
        String moduleName = config.getString("moduleName");
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("moduleName", moduleName);
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("group",config.getString("group"));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        Map<String, String> attachTemplates = getAttachTemplates();
        attachTemplates.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String template, String name) {
                //渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, "UTF-8");
                tpl.merge(context, sw);
                try {
                    //添加到zip
                    String absFileName = getFileName(template, name, config
                            .getString("package"), config.getString("moduleName"));
                    System.out.println(absFileName);
                    String fileName = Arrays.asList(absFileName.split("/")).stream().filter(p -> p.endsWith(".java"))
                            .collect(Collectors.joining());
                    String absDir = absFileName.substring(0, absFileName.indexOf(fileName));
                    System.out.printf("fileName: %s, dir: %s", fileName, absDir);
                    boolean createFileRes = ForFile.createFile(projectPath + moduleName + "/" + absDir, fileName, sw
                            .toString());
                    if (createFileRes) {
                        System.out.printf("success create %s\n", absFileName);
                    } else {
                        System.out.printf("fail create %s\n", absFileName);
                    }
                    zip.putNextEntry(new ZipEntry(absFileName));
                    IOUtils.write(sw.toString(), zip, "UTF-8");
                    IOUtils.closeQuietly(sw);
                    zip.closeEntry();
                } catch (IOException e) {
                    throw new RRException("渲染模板失败，文件名：" + name, e);
                }
            }
        });
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columnsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));
            columnEntity.setLength(column.get("length"));
            columnEntity.setNullable(column.get("nullable"));
            columnEntity.setJpaColumnDefinition(column.get("jpaColumnDefinition"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columnsList.add(columnEntity);
        }
        tableEntity.setColumns(columnsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String projectPath = config.getString("projectPath");
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "com.generator" : mainPath;
        String moduleName = config.getString("moduleName");
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("moduleName", moduleName);
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
            try {
                //添加到zip
                String absFileName = getFileName(template, tableEntity.getClassName(), config
                        .getString("package"), config.getString("moduleName"));
                System.out.println(absFileName);

                String fileName = Arrays.asList(absFileName.split("\\\\")).stream().filter(p -> p.endsWith(".java"))
                        .collect(Collectors.joining());
                String absDir = absFileName.substring(0, absFileName.indexOf(fileName));
                System.out.printf("fileName: %s, dir: %s", fileName, absDir);
                boolean createFileRes = ForFile.createFile(projectPath + moduleName + "/" + absDir, fileName, sw
                        .toString());
                if (createFileRes) {
                    System.out.printf("success create %s\n", absFileName);
                } else {
                    System.out.printf("fail create %s\n", absFileName);
                }
                zip.putNextEntry(new ZipEntry(absFileName));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RRException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String
            moduleName) {


        String clientPackagePath = moduleName + "-client" + File
                .separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            clientPackagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File
                    .separator + "client" + File
                    .separator;
        }
        if (template.contains("BaseQueryDO.java.vm")) {
            return clientPackagePath + "base" + File.separator + className;
        }
        if (template.contains("BaseDO.java.vm")) {
            return clientPackagePath + "base" + File.separator + className;
        }
        if (template.contains("DO.java.vm")) {
            return clientPackagePath + "domain" + File.separator + className + "DO.java";
        }

        if (template.contains("Query.java.vm")) {
            return clientPackagePath + "query" + File.separator + className + "QueryDO.java";
        }


        if (template.contains("CommonCode.java.vm")) {
            return clientPackagePath + "base" + File.separator + className;
        }
        if (template.contains("CommonResult.java.vm")) {
            return clientPackagePath + "base" + File.separator + className;
        }


        String corePackagePath = moduleName + "-service" + File
                .separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            corePackagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File
                    .separator;
        }
        if (template.contains("BaseControllerImpl.java.vm")) {
            return corePackagePath + "service" + File.separator + "base" + File.separator + className;
        }
        if (template.contains("BaseDAO.java.vm")) {
            return corePackagePath + "service" + File.separator + "base" + File.separator + className;
        }
        if (template.contains("BaseServiceAO.java.vm")) {
            return corePackagePath + "service" + File.separator + "base" + File.separator + className;
        }
        if (template.contains("BaseServiceAOImpl.java.vm")) {
            return corePackagePath + "service" + File.separator + "base" + File.separator + className;
        }
        if (template.contains("Dao.java.vm")) {
            return corePackagePath + "dao" + File.separator + className + "Dao.java";
        }

        if (template.contains("Service.java.vm")) {
            return corePackagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("ServiceImpl.java.vm")) {
            return corePackagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }


        if (template.contains("Dao.xml.vm")) {
            return moduleName + "-service" + File
                    .separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator +
                    "mapper" + File
                    .separator +
                    className + "Dao.xml";
        }

        String controllerPackagePath = corePackagePath + "web" + File.separator;

        if (template.contains("Controller.java.vm")) {
            return controllerPackagePath + "controller" + File.separator + className + "Controller.java";
        }

        String apiPackagePath = corePackagePath + "web" + File.separator;

        if (template.contains("Api.java.vm")) {
            return apiPackagePath + "api" + File.separator + className + "Api.java";
        }
        if (template.contains("application-dev.yml.vm")) {
            return moduleName + "-service" + File
                    .separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + className;
        }
        if (template.contains("application-prod.yml.vm")) {
            return moduleName + "-service" + File
                    .separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + className;
        }
        if (template.contains("application-test.yml.vm")) {
            return moduleName + "-service" + File
                    .separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + className;
        }
        if (template.contains("logback.xml.vm")) {
            return moduleName + "-service" + File
                    .separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + className;
        }
        if (template.contains("BootApplication.java.vm")) {
            return corePackagePath + className;
        }
        if (template.contains("client.pom.xml.vm")) {
            return moduleName + "-client" + File
                    .separator + className;
        }
        if (template.contains("service.pom.xml.vm")) {
            return moduleName + "-service" + File
                    .separator + className;
        }
        if (template.contains("master.pom.xml.vm")) {
            return className;
        }
        if (template.contains("DefaultHttpRequestAspect.java.vm")) {
            return corePackagePath + "filter" + File.separator + className;
        }
        if (template.contains("FilterConfiguration.java.vm")) {
            return corePackagePath + "configer" + File.separator + className;
        }
        if (template.contains("ServerProperties.java.vm")) {
            return corePackagePath + "configer" + File.separator + className;
        }
        if (template.contains("SwaggerConfiguration.java.vm")) {
            return corePackagePath + "configer" + File.separator + className;
        }
        if (template.contains("DateUtil.java.vm")) {
            return corePackagePath + "service" + File.separator + "utils" + File.separator + className;
        }
        if (template.contains("DistribID.java.vm")) {
            return corePackagePath + "service" + File.separator + "utils" + File.separator + className;
        }
        if (template.contains("EnvironmentDefine.java.vm")) {
            return corePackagePath + "service" + File.separator + "utils" + File.separator + className;
        }


        System.out.println("不存在的模板");
        return null;
    }
}
