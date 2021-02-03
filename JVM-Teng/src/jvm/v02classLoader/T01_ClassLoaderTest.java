package jvm.v02classLoader;

/**
 * @program: 81plan
 * @description:
 * @author: Mr.Teng
 * @create: 2021-02-02 20:57
 *
 * classLoader   ≤„º∂≤‚ ‘ parent
 **/
public class T01_ClassLoaderTest {

    static {
        System.out.println("init  T01");
    }

    public static void main(String[] args) {
        System.out.println(String.class.getClassLoader());
        System.out.println(String.class.getClassLoader());
        System.out.println(T01_ClassLoaderTest.class.getClassLoader());

        System.out.println(sun.awt.HKSCS.class.getClassLoader());
        System.out.println(sun.net.spi.nameservice.dns.DNSNameService.class.getClassLoader());
    }

    public void gogogo(String form) {
        System.out.println("gogogog"+form);
    }

}
