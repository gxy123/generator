package com.generator.service;

import com.generator.dao.SysGeneratorDao;
import com.generator.utils.GenUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 *
 * @author
 * @date 2016年12月19日 下午3:33:38
 */
@Service
public class SysGeneratorService {

    // 变量field注入
//    @Autowired
//    private SysGeneratorDao sysGeneratorDao;

    // setter注入
//    private SysGeneratorDao sysGeneratorDao;
//
//    @Autowired
//    public void setSysGeneratorDao(SysGeneratorDao sysGeneratorDao) {
//        this.sysGeneratorDao = sysGeneratorDao;
//    }

    private final SysGeneratorDao sysGeneratorDao;

    // 构造器constructor注入
    @Autowired
    public SysGeneratorService(SysGeneratorDao sysGeneratorDao) {
        this.sysGeneratorDao = sysGeneratorDao;
    }

    public List<Map<String, Object>> queryList(Map<String, Object> map) {
        return sysGeneratorDao.queryList(map);
    }

    public int queryTotal(Map<String, Object> map) {
        return sysGeneratorDao.queryTotal(map);
    }

    public Map<String, String> queryTable(String tableName) {
        return sysGeneratorDao.queryTable(tableName);
    }

    public List<Map<String, String>> queryColumns(String tableName) {
        return sysGeneratorDao.queryColumns(tableName);
    }

    public byte[] generatorCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        //生成数据库代码
        for (String tableName : tableNames) {
            //查询表信息
            Map<String, String> table = queryTable(tableName);
            //查询列信息
            List<Map<String, String>> columns = queryColumns(tableName);
            //生成代码
            GenUtils.generatorCode(table, columns, zip);
        }
        //生成框架代码
        GenUtils.generatorAttachCode(zip);
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }
}
