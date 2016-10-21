package com.itspawan.ioc;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class CGLIBInterceptor implements MethodInterceptor {

    private Object object;
    private XMLReader reader;

    public CGLIBInterceptor(Object obj, XMLReader reader) {
        object = obj;
        this.reader = reader;
    }

    public Object intercept(Object proxy, Method method, Object[] args,
                            MethodProxy methodProxy) throws Throwable {

        if (method.getName().startsWith("get")) {
            Object rtnObject = methodProxy.invokeSuper(proxy, args);
            if (rtnObject == null) {
                String name = method.getName();
                String nameOfBean = name.substring(3, name.length()).toLowerCase();
                rtnObject = reader.getBean(nameOfBean);
                return rtnObject;
            }
        }
        return methodProxy.invokeSuper(proxy, args);
    }


}
