package com.springmvc.framework.context;

import com.springmvc.framework.annotation.ZyAutowird;
import com.springmvc.framework.annotation.ZyController;
import com.springmvc.framework.annotation.ZyService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sunyong on 2018/9/15.
 */
public class ZyApplicationContext {

    private Properties prop = new Properties();

    //类似于内部的消息，我们在外面是看不到的
    //我们能看到的是只有ioc容器， getBean方法来间接调用的
    private List<String> classCache = new ArrayList<String>();

    private Map<String, Object> instanceMapping = new ConcurrentHashMap<String, Object>();

    public ZyApplicationContext(String location)  {
        // 定位 加载 注册 初始化 注入

        try {
            // 1、定位
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);

            // 2、载入
            prop.load(is);

            // 3、注册
            String packageName = prop.getProperty("scanPackage");
            doRegister(packageName);

            // 4、实例化需要ioc的对象（就是加了@service @controller），只要循环class了
            doCreateBean();

            // 5、注册
            populate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 把所有符合条件的class取出来，注册到缓存里去
    private void doRegister(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doRegister(packageName + "." + file.getName());
            } else {
                classCache.add(packageName + "." + file.getName().replaceAll(".class", "").trim());
            }
        }

    }

    private void doCreateBean() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // 检测看有没有注册信息，注册信息里保存了所有的class名字
        // BeanDefinition保存了类的名字，也保存类和类之间的关系（Map/List/Set/ref/parent）
        if (classCache.size() == 0) {
            return;
        }

        for (String className : classCache) {
            // 知道这里有个套路
            Class<?> clazz = Class.forName(className);

            // 哪个类需要初始化，哪个类不需要初始化
            // 只要加了@service @Controller都要初始化
            if (clazz.isAnnotationPresent(ZyController.class)) {
                // 名字起啥，默认就是类名首字母小写
                String id = lowerFirstChar(clazz.getSimpleName());
                instanceMapping.put(id, clazz.newInstance());
            } else if (clazz.isAnnotationPresent(ZyService.class)) {
                ZyService zyService = clazz.getAnnotation(ZyService.class);
                // 如果设置了自定义名字，就优先使用它自己的名字
                String id = zyService.value().trim();
                if (!"".equals(id)) {
                    instanceMapping.put(id, clazz.newInstance());
                    continue;
                }

                //如果是空的，就用默认规则
                //1、类名首字母小写
                //如果这个类是接口
                //2、可以根据类型类匹配
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> i : interfaces) {
                    instanceMapping.put(i.getName(), clazz.newInstance());
                }
            } else {
                continue;
            }
        }

    }

    // 依赖注入
    private void populate() {
        // 判断ioc容器里有没有东西
        if (instanceMapping.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : instanceMapping.entrySet()) {
            // 把所有的属性全部取出来，包括私有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(ZyAutowird.class)) {
                    continue;
                }
                ZyAutowird zyAutowird = field.getAnnotation(ZyAutowird.class);
                String id = zyAutowird.value().trim();
                //如果id为空，也就是说自己没有设置，默认根据类型来注入
                if ("".equals(id)) {
                    id = field.getType().getName();
                }
                // 把私有变量开放访问权限
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), instanceMapping.get(id));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * 将首字母小写
     *
     * @param str
     * @return
     */
    private String lowerFirstChar(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public Map<String, Object> getAll() {
        return instanceMapping;
    }

}
