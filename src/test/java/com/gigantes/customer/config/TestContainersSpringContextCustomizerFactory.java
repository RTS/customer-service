package com.gigantes.customer.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.KafkaContainer;

public class TestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

    private Logger log = LoggerFactory.getLogger(TestContainersSpringContextCustomizerFactory.class);

    private static KafkaTestContainer kafkaBean;
    private static CouchbaseTestContainer couchbaseBean;

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            TestPropertyValues testValues = TestPropertyValues.empty();
            EmbeddedCouchbase couchbaseAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedCouchbase.class);
            if (null != couchbaseAnnotation) {
                log.debug("detected the EmbeddedCouchbase annotation on class {}", testClass.getName());
                log.info("Warming up the Couchbase database");
                if (null == couchbaseBean) {
                    couchbaseBean = beanFactory.createBean(CouchbaseTestContainer.class);
                    beanFactory.registerSingleton(CouchbaseTestContainer.class.getName(), couchbaseBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(CouchbaseTestContainer.class.getName(), couchbaseBean);
                }
                testValues =
                    testValues.and("spring.couchbase.connection-string=" + couchbaseBean.getCouchbaseContainer().getConnectionString());
                testValues = testValues.and("spring.couchbase.username=" + couchbaseBean.getCouchbaseContainer().getUsername());
                testValues = testValues.and("spring.couchbase.password=" + couchbaseBean.getCouchbaseContainer().getPassword());
                testValues = testValues.and("jhipster.database.couchbase.bucket-name=" + couchbaseBean.getBucketName());
                testValues = testValues.and("jhipster.database.couchbase.scope-name=testScope");
            }
            EmbeddedKafka kafkaAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedKafka.class);
            if (null != kafkaAnnotation) {
                log.debug("detected the EmbeddedKafka annotation on class {}", testClass.getName());
                log.info("Warming up the kafka broker");
                if (null == kafkaBean) {
                    kafkaBean = beanFactory.createBean(KafkaTestContainer.class);
                    beanFactory.registerSingleton(KafkaTestContainer.class.getName(), kafkaBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(KafkaTestContainer.class.getName(), kafkaBean);
                }
                testValues =
                    testValues.and(
                        "spring.cloud.stream.kafka.binder.brokers=" +
                        kafkaBean.getKafkaContainer().getHost() +
                        ':' +
                        kafkaBean.getKafkaContainer().getMappedPort(KafkaContainer.KAFKA_PORT)
                    );
            }
            testValues.applyTo(context);
        };
    }
}
