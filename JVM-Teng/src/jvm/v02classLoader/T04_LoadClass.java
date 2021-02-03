package jvm.v02classLoader;

import com.mashibing.jvm.c2_classloader.T005_LoadClassByHand;

/**
 * @program: 81plan
 * @description: º”‘ÿ÷∏∂®class
 * @author: Mr.Teng
 * @create: 2021-02-03 13:52
 **/
public class T04_LoadClass {

    public static void main(String[] args) throws ClassNotFoundException {
        Class clazz = T005_LoadClassByHand.class.getClassLoader().loadClass("v02classLoader.T02_ClassLoaderScope");
        System.out.println(clazz.getName());
        System.out.println(clazz.getClassLoader());
    }
}
