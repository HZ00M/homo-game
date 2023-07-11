package com.homo.common.proxy.util;

import lombok.experimental.UtilityClass;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ProxyCheckParamUtils {
    public   boolean checkIsNullOrEmpty(String...strs){
        for(String str : strs){
            if(str == null || "".equals(str)){
                return true;
            }
        }
        return false;
    }
    public    boolean checkIsNull(Object...objs){
        for(Object obj : objs){
            if(obj == null ){
                return true;
            }
        }
        return false;
    }
    public  boolean checkObjectIsNull(Object obj) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            String name = field.getName();
            String methodName = name.substring(0,1).toUpperCase() + name.substring(1);
            Method getMethod = obj.getClass().getMethod("get" + methodName);
            Object invoke = getMethod.invoke(obj);
            if(isEmpty(invoke)){
                return true;
            }
        }
        return false;
    }

    /**对象判空*/
    public  boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if ((obj instanceof List)) {
            return ((List) obj).isEmpty();
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        }
        if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        }
        if (obj instanceof Object[]) {
            Object[] object = (Object[]) obj;
            if (object.length == 0) {
                return true;
            }
            boolean empty = true;
            for (int i = 0; i < object.length; i++) {
                if (!isEmpty(object[i])) {
                    empty = false;
                    break;
                }
            }
            return empty;
        }
        return false;
    }
}
