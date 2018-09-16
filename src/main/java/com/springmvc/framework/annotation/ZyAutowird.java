package com.springmvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by sunyong on 2018/9/15.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZyAutowird {

    String value() default "";
}
