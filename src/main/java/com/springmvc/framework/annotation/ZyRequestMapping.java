package com.springmvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by sunyong on 2018/9/15.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZyRequestMapping {
    String value() default "";
}
