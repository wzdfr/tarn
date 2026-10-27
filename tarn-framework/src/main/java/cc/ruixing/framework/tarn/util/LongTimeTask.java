package cc.ruixing.framework.tarn.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class LongTimeTask extends Thread {
    private static Map<Long, LongTimeTask> map = new HashMap<Long, LongTimeTask>();

    private static long IDBase = System.currentTimeMillis();
    private static final int MaxListSize = 1000;
    private long id;
    private ArrayList<String> list = new ArrayList();
    protected int percent;
    protected String currentInfo;
    private String finishedInfo;
    protected ArrayList<String> errors = new ArrayList();
    private boolean stopFlag;
    private String type;
    private long stopTime = System.currentTimeMillis() + 1440000L;

    private static ReentrantLock lock = new ReentrantLock();

//    public static LongTimeTask createEmptyInstance() { // Byte code:
//        //   0: new 59
//        //   3: dup
//        //   4: iconst_0
//        //   5: invokespecial 61
//        //   8: areturn
//    }

    public static LongTimeTask getInstanceById(long id) {
        return (LongTimeTask) map.get(new Long(id));
    }

    public static Collection<LongTimeTask> getAllInstance() {
        return map.values();
    }

    public static void removeInstanceById(long id) {
        map.remove(new Long(id));
    }

    public static String cancelByType(String type) {
        String message = "取消:" + type;
        LongTimeTask ltt = getInstanceByType(type);
        if (ltt != null) {
            ltt.stopTask();
            message = "停止";
        }
        return message;
    }

    public static LongTimeTask getInstanceByType(String type) {
        if (StringUtil.isNotEmpty(type)) {
            long current = System.currentTimeMillis();
            for (Long key : map.keySet()) {
                LongTimeTask ltt = (LongTimeTask) map.get(key);
                if (type.equals(ltt.getType())) {
                    if (current - ltt.stopTime > 60000L) {
                        map.remove(key);
                        return null;
                    }
                    return ltt;
                }
            }
        }
        return null;
    }

    public LongTimeTask() {
        this(true);
    }

    private LongTimeTask(boolean flag) {
        if (flag) {
            setName("LongTimeTask Thread");
            this.id = (IDBase++);
            map.put(new Long(this.id), this);
            clearStopedTask();
        }
    }

    private void clearStopedTask() {
        lock.lock();
        try {
            long current = System.currentTimeMillis();
            for (Long k : map.keySet()) {
                LongTimeTask ltt = (LongTimeTask) map.get(k);
                if (current - ltt.stopTime > 60000L)
                    map.remove(k);
            }
        } finally {
            lock.unlock();
        }
    }

    public long getTaskID() {
        return this.id;
    }

    public void info(String message) {
        LogUtil.info(message);
        this.list.add(message);
        if (this.list.size() > 1000)
            this.list.remove(0);
    }

    public String[] getMessages() {
        String[] arr = new String[this.list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ((String) this.list.get(i));
        }
        this.list.clear();
        return arr;
    }

    public void run() {
        if (StringUtil.isNotEmpty(this.type)) {
            LongTimeTask ltt = getInstanceByType(this.type);
            if ((ltt != null) && (ltt != this))
                return;
        }
        try {
            execute();
        } catch (Exception ie) {
            interrupt();
        } finally {
            this.stopTime = System.currentTimeMillis();
        }
    }

    public abstract void execute();

    public boolean checkStop() {
        return this.stopFlag;
    }

    public void stopTask() {
        clearStopedTask();
        this.stopFlag = true;
    }

    public int getPercent() {
        return this.percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setCurrentInfo(String currentInfo) {
        this.currentInfo = currentInfo;
        LogUtil.info(currentInfo);
    }

    public String getCurrentInfo() {
        return this.currentInfo;
    }

    public void setFinishedInfo(String finishedInfo) {
        setPercent(100);
        this.finishedInfo = finishedInfo;
        LogUtil.info(finishedInfo);
    }

    public String getFinishedInfo() {
        return this.finishedInfo;
    }


    public void addError(String error) {
        this.errors.add(error);
    }

    public String getAllErrors() {
        if (this.errors.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
//        StringFormat sf = new StringFormat(LangMapping.get("Framework.Task.TotleErrors"), new Object[]{Integer.valueOf(this.errors.size())});
//        sb.append(sf.toString() + ":<br>");
        for (int i = 0; i < this.errors.size(); i++) {
            sb.append(i + 1);
            sb.append(": ");
            sb.append((String) this.errors.get(i));
            sb.append("<br>");
        }
        return sb.toString();
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}