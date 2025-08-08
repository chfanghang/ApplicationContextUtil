package com.fhedu.springJdbc;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;

/**
 * Spring容器工具类，用于安全加载配置文件、管理容器生命周期并便捷获取Bean
 * 支持自动关闭容器（实现AutoCloseable），避免资源泄漏；支持多配置文件加载
 *
 * @author fanghang
 * @date 2025-08-08
 */
public class ApplicationContextUtil<T> implements Closeable {

    // Spring容器实例（ApplicationContext本身线程安全，可安全共享）
    private final ApplicationContext ioc;

    // 目标Bean实例（泛型约束，确保类型安全）
    private final T bean;


    /**
     * 构造方法：加载多个Spring配置文件并获取指定类型的Bean
     *
     * @param beanClass  要获取的Bean的Class对象（非空，如MonsterDao.class）
     * @param configFiles 类路径下的Spring配置文件路径（可变参数，至少一个，如"jdbc.xml", "service.xml"）
     * @throws IllegalArgumentException 当configFiles为空或beanClass为null时抛出
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 当容器中无指定类型的Bean时抛出
     */
    public ApplicationContextUtil(Class<T> beanClass, String... configFiles) {
        // 参数校验：提前拦截无效参数，避免Spring内部抛出模糊异常
        Assert.notEmpty(configFiles, "配置文件路径不能为空！至少需要一个Spring配置文件");
        Assert.notNull(beanClass, "Bean的Class对象不能为null！");

        // 加载配置文件，初始化Spring容器（支持多文件）
        this.ioc = new ClassPathXmlApplicationContext(configFiles);

        // 获取Bean（Spring会自动检查类型匹配，无Bean时抛出NoSuchBeanDefinitionException）
        this.bean = ioc.getBean(beanClass);
    }


    /**
     * 静态工具方法：快速获取指定类型的Bean（适用于临时获取单个Bean的场景）
     * 注意：此方法会创建并自动关闭容器，适合单次使用；频繁调用建议复用ApplicationContextUtil实例
     *
     * @param beanClass  要获取的Bean的Class对象
     * @param configFiles 配置文件路径（可变参数）
     * @return 容器中匹配类型的Bean
     */
    public static <T> T getBean(Class<T> beanClass, String... configFiles) {
        // 利用try-with-resources自动关闭容器
        try (ApplicationContextUtil<T> util = new ApplicationContextUtil<>(beanClass, configFiles)) {
            return util.getBean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取Spring容器实例（如需手动获取其他Bean时使用）
     *
     * @return 初始化后的ApplicationContext
     */
    public ApplicationContext getIoc() {
        return ioc;
    }


    /**
     * 获取当前工具类管理的目标Bean
     *
     * @return 泛型约束的Bean实例（非null）
     */
    public T getBean() {
        return bean;
    }


    /**
     * 关闭Spring容器（释放资源，如数据库连接池等）
     * 实现Closeable接口，支持try-with-resources语法自动调用
     */
    @Override
    public void close() throws IOException {
        if (ioc instanceof ClassPathXmlApplicationContext) {
            // 关闭容器（ClassPathXmlApplicationContext实现了Closable）
            ((ClassPathXmlApplicationContext) ioc).close();
        }
    }
}
