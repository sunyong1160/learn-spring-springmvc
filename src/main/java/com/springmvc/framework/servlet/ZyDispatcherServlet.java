package com.springmvc.framework.servlet;

import com.springmvc.framework.annotation.ZyController;
import com.springmvc.framework.annotation.ZyRequestMapping;
import com.springmvc.framework.annotation.ZyRequestParam;
import com.springmvc.framework.context.ZyApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sunyong on 2018/9/15.
 */
public class ZyDispatcherServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";

    private List<Handler> handlerMapping = new ArrayList<Handler>();

    private Map<Handler, HandlerAdapter> adapterMapping = new HashMap<Handler, HandlerAdapter>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        // IOC容器初始化
        try {
            System.out.println(123);
            ZyApplicationContext context = new ZyApplicationContext(config.getInitParameter(LOCATION));


            //请求解析
            initMultipartResolver(context);
            //多语言、国际化
            initLocaleResolver(context);
            //主题View层的
            initThemeResolver(context);

            //============== 重要 ================
            //解析url和Method的关联关系
            initHandlerMappings(context);
            //适配器（匹配的过程）
            initHandlerAdapters(context);
            //============== 重要 ================


            //异常解析
            initHandlerExceptionResolvers(context);
            //视图转发（根据视图名字匹配到一个具体模板）
            initRequestToViewNameTranslator(context);

            //解析模板中的内容（拿到服务器传过来的数据，生成HTML代码）
            initViewResolvers(context);

            initFlashMapManager(context);

            System.out.println("GPSpring MVC is init.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    //在这里调用自己写的Controller的方法
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Msg :" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        try {
            // 先取出来一个Handler，从handlerMapping取
            Handler handler = getHandler(req);
            if (handler == null) {
                resp.getWriter().write("404 Not Found");
                return;
            }
            // 再取出一个适配器
            // 再由适配取调用我们具体的方法
            HandlerAdapter ha = getHandlerAdapter(handler);
            ha.handle(req, resp, handler);
        } catch (Exception e) {

        }
    }

    private HandlerAdapter getHandlerAdapter(Handler handler) {
        if (adapterMapping.isEmpty()) {
            return null;
        }
        return adapterMapping.get(handler);
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }


    private void initMultipartResolver(ZyApplicationContext context) {

    }

    private void initLocaleResolver(ZyApplicationContext context) {

    }

    private void initThemeResolver(ZyApplicationContext context) {

    }

    private void initHandlerMappings(ZyApplicationContext context) {
        Map<String, Object> ioc = context.getAll();
        if (ioc.isEmpty()) {
            return;
        }
        // 只要是由Controller修饰类，里面的方法全部取出来
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(ZyController.class)) {
                continue;
            }
            String url = "";
            if (clazz.isAnnotationPresent(ZyRequestMapping.class)) {
                ZyRequestMapping zyRequestMapping = clazz.getAnnotation(ZyRequestMapping.class);
                url = zyRequestMapping.value().trim();
            }
            // 扫描Controller下面的所有方法
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(ZyRequestMapping.class)) {
                    continue;
                }
                ZyRequestMapping zyRequestMapping = method.getAnnotation(ZyRequestMapping.class);
                String regex = (url + zyRequestMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new Handler(entry.getValue(), method, pattern));

                System.out.println("Mapping: " + regex + " " + method.toString());
            }

        }

    }

    //适配器（匹配的过程）
    // 主要是用来动态匹配我们的参数
    private void initHandlerAdapters(ZyApplicationContext context) {
        if (handlerMapping.isEmpty()) {
            return;
        }
        //参数类型作为key，参数的索引号作为值
        Map<String, Integer> paramMapping = new HashMap<String, Integer>();

        // 只需取出来具体的某个方法
        for (Handler handler : handlerMapping) {
            //把这个方法上面所有的参数全部获取到
            Class<?>[] parameterTypes = handler.method.getParameterTypes();

            //有顺序，但是通过反射，没法拿到我们的参数名字
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];

                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramMapping.put(type.getName(), i);
                }
            }

            //这里是匹配Request和Response
            Annotation[][] pa = handler.method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof ZyRequestParam) {
                        String paramName = ((ZyRequestParam) a).value();
                        if (!"".equals(paramName.trim())) {
                            paramMapping.put(paramName, i);
                        }
                    }
                }
            }
            adapterMapping.put(handler, new HandlerAdapter(paramMapping));
        }
    }

    private void initHandlerExceptionResolvers(ZyApplicationContext context) {

    }

    private void initRequestToViewNameTranslator(ZyApplicationContext context) {

    }

    private void initViewResolvers(ZyApplicationContext context) {

    }

    private void initFlashMapManager(ZyApplicationContext context) {

    }
}

/**
 * handlerMapping定义
 */
class Handler {

    protected Object controller;
    protected Method method;
    protected Pattern pattern;

    protected Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
    }
}

/**
 * 方法适配器
 */
class HandlerAdapter {

    private Map<String, Integer> paramMapping;

    public HandlerAdapter(Map<String, Integer> paramMapping) {
        this.paramMapping = paramMapping;
    }

    //主要目的是用反射调用url对应的method
    public void handle(HttpServletRequest req, HttpServletResponse resp, Handler handler) throws InvocationTargetException, IllegalAccessException {

//为什么要传req、为什么要穿resp、为什么传handler
        Class<?>[] paramTypes = handler.method.getParameterTypes();

        //要想给参数赋值，只能通过索引号来找到具体的某个参数

        Object[] paramValues = new Object[paramTypes.length];

        Map<String, String[]> params = req.getParameterMap();

        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

            if (!this.paramMapping.containsKey(param.getKey())) {
                continue;
            }

            int index = this.paramMapping.get(param.getKey());

            //单个赋值是不行的
            paramValues[index] = castStringValue(value, paramTypes[index]);
        }

        //request 和 response 要赋值
        String reqName = HttpServletRequest.class.getName();
        if (this.paramMapping.containsKey(reqName)) {
            int reqIndex = this.paramMapping.get(reqName);
            paramValues[reqIndex] = req;
        }


        String resqName = HttpServletResponse.class.getName();
        if (this.paramMapping.containsKey(resqName)) {
            int respIndex = this.paramMapping.get(resqName);
            paramValues[respIndex] = resp;
        }

        handler.method.invoke(handler.controller, paramValues);
    }

    private Object castStringValue(String value, Class<?> clazz) {
        if (clazz == String.class) {
            return value;
        } else if (clazz == Integer.class) {
            return Integer.valueOf(value);
        } else if (clazz == int.class) {
            return Integer.valueOf(value).intValue();
        } else {
            return null;
        }
    }
}
