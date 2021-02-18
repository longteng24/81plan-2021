package jvm.v04_gc;

/**
 * 测试垃圾回收   根可达算法
 *   对象 可通过finalize 实现自救  仅可使用一次
 *   如果对象被判定为有必要执行finalize（）方法，那么这个对象将会被放在名为F-Queue的队列中，并在稍后一条由虚拟机自动建立的、低优先级的Finalize线程去执行。
 *    参考：https://blog.csdn.net/lpw_cn/article/details/84423500
 */
public class FinalizeEscapeGC {

    private static FinalizeEscapeGC SAVE_HOOK = null;

    private void isAlive() {
        System.out.println("Yes,I am still alive!");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("finalize() method executed!");
        FinalizeEscapeGC.SAVE_HOOK = this;
    }

    public static void main(String args[]) throws Throwable {
        SAVE_HOOK = new FinalizeEscapeGC();
        // 对象第一次成功拯救自己
        SAVE_HOOK = null;
        // 调用该方法建议系统执行垃圾清理，但也并不一定执行
        System.gc();
        // 因为Finalize线程优先级较低，暂停0.5秒以等待它
        Thread.sleep(500);
        if (SAVE_HOOK != null) {
            SAVE_HOOK.isAlive();
        } else {
            System.out.println("No, I am dead!");
        }
        System.out.println("============");

        // 下面这段代码与上面完全相同，但这次却自救失败了
        SAVE_HOOK = null;
        System.gc();
        Thread.sleep(500);
        if (SAVE_HOOK != null) {
            SAVE_HOOK.isAlive();
        } else {
            System.out.println("No, I am dead!");
        }
    }
}
