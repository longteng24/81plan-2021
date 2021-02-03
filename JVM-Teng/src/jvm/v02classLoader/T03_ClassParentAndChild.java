package jvm.v02classLoader;

/**
 * @program: 81plan
 * @description: ≤‚ ‘classLoader≤„º∂πÿœµ
 * @author: Mr.Teng
 * @create: 2021-02-03 13:44
 **/
public class T03_ClassParentAndChild {

    public static void main(String[] args) {
        System.out.println(T03_ClassParentAndChild.class.getClassLoader());

        System.out.println("======================");
        System.out.println(T03_ClassParentAndChild.class.getClassLoader().getParent());
        System.out.println(T03_ClassParentAndChild.class.getClassLoader().getParent().getClass());

        System.out.println("======================");
        System.out.println(T03_ClassParentAndChild.class.getClassLoader().getParent().getParent());
        System.out.println(T03_ClassParentAndChild.class.getClassLoader().getParent().getParent().getClass());
    }
}
