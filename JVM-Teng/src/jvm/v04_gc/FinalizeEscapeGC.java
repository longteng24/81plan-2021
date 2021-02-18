package jvm.v04_gc;

/**
 * ������������   ���ɴ��㷨
 *   ���� ��ͨ��finalize ʵ���Ծ�  ����ʹ��һ��
 *   ��������ж�Ϊ�б�Ҫִ��finalize������������ô������󽫻ᱻ������ΪF-Queue�Ķ����У������Ժ�һ����������Զ������ġ������ȼ���Finalize�߳�ȥִ�С�
 *    �ο���https://blog.csdn.net/lpw_cn/article/details/84423500
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
        // �����һ�γɹ������Լ�
        SAVE_HOOK = null;
        // ���ø÷�������ϵͳִ������������Ҳ����һ��ִ��
        System.gc();
        // ��ΪFinalize�߳����ȼ��ϵͣ���ͣ0.5���Եȴ���
        Thread.sleep(500);
        if (SAVE_HOOK != null) {
            SAVE_HOOK.isAlive();
        } else {
            System.out.println("No, I am dead!");
        }
        System.out.println("============");

        // ������δ�����������ȫ��ͬ�������ȴ�Ծ�ʧ����
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
