package com.xj.router.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * 博客地址：http://blog.csdn.net/gdutxiaoxu
 * @author xujun
 * @time 2019-11-13 20:37.
 */
@Retention(RetentionPolicy.CLASS)
public @interface Modules {
    String[] value();
}
