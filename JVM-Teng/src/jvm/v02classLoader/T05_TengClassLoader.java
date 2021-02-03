package jvm.v02classLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @program: 81plan
 * @description: 自定义ClassLoader
 * @author: Mr.Teng
 * @create: 2021-02-03 13:56
 **/
public class T05_TengClassLoader extends ClassLoader {

    private  static  final String FILE_DIR_PATH = "F:/test/";


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        File f = new File(FILE_DIR_PATH, name.replace(".", "/").concat(".class"));


        try (
                FileInputStream fis = new FileInputStream(f);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ) {
//
//            byte[] bytes = getClassBytes(f);
//            return defineClass(name, bytes, 0, bytes.length);

            int b = 0;
            byte[] buf = new byte[1024];
            while ((b=fis.read(buf)) !=-1) {
                baos.write(buf,0,b);
            }
            byte[] bytes = baos.toByteArray();

            return defineClass(name, bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.findClass(name);
    }

    private byte[] getClassBytes(File file) throws Exception {
        // 这里要读入.class的字节，因此要使用字节流
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel wbc = Channels.newChannel(baos);
        ByteBuffer by = ByteBuffer.allocate(1024);

        while (true)
        {
            int i = fc.read(by);
            if (i == 0 || i == -1)
                break;
            by.flip();
            wbc.write(by);
            by.clear();
        }

        fis.close();

        return baos.toByteArray();
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        ClassLoader l = new T05_TengClassLoader();

        Class<?> clazz = l.loadClass("com.xrq.classloader.Person");

        System.out.println(clazz);
        System.out.println(clazz.getClassLoader());

        System.out.println(clazz.getClass());
        System.out.println(clazz.getClass().getClassLoader());

        System.out.println(clazz.getClassLoader().getParent());
        System.out.println(getSystemClassLoader());



    }
}
