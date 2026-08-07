package com.trendyol.openapiaggregator.configuration;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

public final class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        Resource source = resource.getResource();
        yaml.setResources(source);
        return new PropertiesPropertySource(name != null ? name : source.getFilename(), yaml.getObject());
    }
}
