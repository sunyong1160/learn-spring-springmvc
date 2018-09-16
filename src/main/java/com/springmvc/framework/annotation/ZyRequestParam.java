package com.springmvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by sunyong on 2018/9/15.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZyRequestParam {

    String value() default "";

    boolean required() default true;
}
