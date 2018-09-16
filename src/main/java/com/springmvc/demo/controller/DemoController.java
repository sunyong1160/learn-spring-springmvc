package com.springmvc.demo.controller;

import com.springmvc.demo.service.DemoService;
import com.springmvc.demo.service.NameService;
import com.springmvc.framework.annotation.ZyAutowird;
import com.springmvc.framework.annotation.ZyController;
import com.springmvc.framework.annotation.ZyRequestMapping;
import com.springmvc.framework.annotation.ZyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by sunyong on 2018/9/15.
 */
@ZyController
@ZyRequestMapping("/demo")
public class DemoController {

    @ZyAutowird
    private DemoService demoService;

    @ZyAutowird("nameService123")
    private NameService nameService;

    @ZyRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @ZyRequestParam("name") String name) {
        out(response, name);
    }

    public void out(HttpServletResponse response, String str) {
        try {
            response.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
