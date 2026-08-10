package com.trendyol.openapiaggregator.configuration;

import com.trendyol.openapiaggregator.exception.OpenApiAggregatorException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.util.Properties;

public final class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        Resource source = resource.getResource();
        yaml.setResources(source);
        String sourceName = name != null ? name : source.getFilename();
        if (sourceName == null) {
            throw new OpenApiAggregatorException("YAML property source filename is missing");
        }
        Properties properties = yaml.getObject();
        if (properties == null) {
            throw new OpenApiAggregatorException("YAML property source did not produce properties");
        }
        return new PropertiesPropertySource(sourceName, properties);
    }
}
