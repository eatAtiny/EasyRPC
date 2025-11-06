package com.easy.simple.rpc.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * 配置工具类，支持properties、yml、yaml格式配置文件
 */
public class ConfigUtils {

    /**
     * 加载配置对象
     *
     * @param tClass 配置类
     * @param prefix 前缀
     * @param <T> 配置类类型
     * @return 配置对象实例
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix) {
        return loadConfig(tClass, prefix, "");
    }

    /**
     * 加载配置对象，支持区分环境
     *
     * @param tClass 配置类
     * @param prefix 前缀
     * @param environment 环境
     * @param <T> 配置类类型
     * @return 配置对象实例
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix, String environment) {
        // 优先尝试加载YAML格式配置文件
        T yamlConfig = loadYamlConfig(tClass, prefix, environment);
        if (yamlConfig != null) {
            return yamlConfig;
        }
        
        // 如果YAML配置不存在，则加载properties格式配置文件
        StringBuilder configFileBuilder = new StringBuilder("application");
        if (StrUtil.isNotBlank(environment)) {
            configFileBuilder.append("-").append(environment);
        }
        configFileBuilder.append(".properties");
        Props props = new Props(configFileBuilder.toString());
        return props.toBean(tClass, prefix);
    }
    
    /**
     * 加载YAML格式配置文件
     *
     * @param tClass 配置类
     * @param prefix 前缀
     * @param environment 环境
     * @param <T> 配置类类型
     * @return 配置对象实例，如果配置文件不存在则返回null
     */
    private static <T> T loadYamlConfig(Class<T> tClass, String prefix, String environment) {
        // 尝试加载application.yml
        String yamlFileName = "application.yml";
        if (StrUtil.isNotBlank(environment)) {
            yamlFileName = "application-" + environment + ".yml";
        }
        
        // 检查YAML文件是否存在
        try {
            InputStream yamlStream = ResourceUtil.getStream(yamlFileName);
            if (yamlStream != null) {
                try {
                    Yaml yaml = new Yaml();
                    Map<String, Object> yamlData = yaml.load(yamlStream);
                    return convertYamlToBean(yamlData, tClass, prefix);
                } catch (Exception e) {
                    // YAML解析失败，尝试加载application.yaml
                    try {
                        String yamlAltFileName = yamlFileName.replace(".yml", ".yaml");
                        InputStream yamlAltStream = ResourceUtil.getStream(yamlAltFileName);
                        if (yamlAltStream != null) {
                            Yaml yaml = new Yaml();
                            Map<String, Object> yamlData = yaml.load(yamlAltStream);
                            return convertYamlToBean(yamlData, tClass, prefix);
                        }
                    } catch (Exception ex) {
                        // 忽略异常，继续尝试properties文件
                    }
                }
            }
        } catch (Exception e) {
            // 忽略文件不存在的异常，继续尝试其他格式
        }
        
        // 尝试加载application.yaml
        String yamlAltFileName = "application.yaml";
        if (StrUtil.isNotBlank(environment)) {
            yamlAltFileName = "application-" + environment + ".yaml";
        }
        
        try {
            InputStream yamlAltStream = ResourceUtil.getStream(yamlAltFileName);
            if (yamlAltStream != null) {
                try {
                    Yaml yaml = new Yaml();
                    Map<String, Object> yamlData = yaml.load(yamlAltStream);
                    return convertYamlToBean(yamlData, tClass, prefix);
                } catch (Exception e) {
                    // 忽略异常，继续尝试properties文件
                }
            }
        } catch (Exception e) {
            // 忽略文件不存在的异常，继续尝试properties文件
        }
        
        return null;
    }
    
    /**
     * 将YAML数据转换为配置对象
     *
     * @param yamlData YAML数据
     * @param tClass 配置类
     * @param prefix 前缀
     * @param <T> 配置类类型
     * @return 配置对象实例
     */
    private static <T> T convertYamlToBean(Map<String, Object> yamlData, Class<T> tClass, String prefix) {
        try {
            // 根据前缀获取对应的配置数据
            Map<String, Object> configData = getConfigDataByPrefix(yamlData, prefix);
            
            // 使用Hutool的BeanUtil将Map转换为Bean
            return cn.hutool.core.bean.BeanUtil.toBean(configData, tClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert YAML data to " + tClass.getSimpleName(), e);
        }
    }
    
    /**
     * 根据前缀获取配置数据
     *
     * @param yamlData 完整的YAML数据
     * @param prefix 前缀
     * @return 对应前缀的配置数据
     */
    private static Map<String, Object> getConfigDataByPrefix(Map<String, Object> yamlData, String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return yamlData;
        }
        
        String[] prefixParts = prefix.split("\\.");
        Map<String, Object> currentData = yamlData;
        
        for (String part : prefixParts) {
            if (currentData.containsKey(part)) {
                Object value = currentData.get(part);
                if (value instanceof Map) {
                    currentData = (Map<String, Object>) value;
                } else {
                    throw new RuntimeException("Invalid prefix '" + prefix + "' in YAML configuration");
                }
            } else {
                throw new RuntimeException("Prefix '" + prefix + "' not found in YAML configuration");
            }
        }
        
        return currentData;
    }
}