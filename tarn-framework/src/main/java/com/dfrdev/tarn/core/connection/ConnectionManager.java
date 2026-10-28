package com.dfrdev.tarn.core.connection;


import com.dfrdev.tarn.util.LogUtil;


/**
 * Simple管理连接池类。
 *
 * @author dfr
 */
public class ConnectionManager extends Thread {
    //1 * 60 * 1000
    protected final long timeDiff = ConnectionConfig.getConnectionCheck();//1小时60分钟，1分钟60秒，1秒1000毫秒
   TarnDataSource jds = TarnDataSource.getInstance();
    int dbInitConns = ConnectionConfig.getInitialSize();
    int lows = ConnectionConfig.getMinIdle();
    int maxs = ConnectionConfig.getMaxActive();
    int poolmaxs = ConnectionConfig.getMaxIdle();
    private boolean isAlive = false;

    public synchronized void run() {
        isAlive = true;
        try {
            sleep(timeDiff*2);
        } catch (InterruptedException e) {
            LogUtil.error("连接管理线程启动等待中断: " + e.getMessage(), e);
        }
        while (true) {
            int poolTol = jds.getPoolSize();
            int poolAna = jds.getPoolSizeAna();
            //  int poolUnana = cps.getPoolSizeUnana();
            //检测连接池内的可用连接不能小于预期配置好的初始连接数

            if (poolAna < lows) {
                int len = lows - poolAna;
                for (int i = 0; i < len; i++) {
                  ConnectionInfo ci = new ConnectionInfo(jds.newConnection(), "AVAILABLE");
                    jds.connectionPool.add(ci);
                }
            //System.out.println("可用链接小于设置的链接："+poolAna+"___:"+lows);
            }
            if (poolAna > maxs) {
                int len = poolAna - maxs;
                for (int i = 0; i < len; i++) {
                    jds.close(jds.getConnection());
                }
                //  System.out.println("可用链接大于设置的链接："+poolTol+"___:"+maxs);
            }
            if (poolTol > poolmaxs) {
                int len = poolTol - poolmaxs;
                for (int i = 0; i < len; i++) {
                    jds.close(jds.getConnection());
                }
            }
//            if (poolAna >= lows || poolAna < maxs) {
//                System.out.println(timeDiff + ":CCCCCCCCCCCCC");
//                // isAlive = false;
//                break;
//            }
            //检测链接池的链接是否达到顶峰，这种情况的解释是：设想一下，假如同时有1000个（链接）人在线访问，那么这1000个人同时突然断开了连接，1000个链接回收到了可用链接，长期这样，将会导致服务器消耗不了
//            if (poolTol > poolmaxs || poolTol > maxs) {
//                break;
//            }
//            //检测已使用的链接是否被释放，若是被释放则要回收可用链接池内
//
//            if ((poolUnana + poolAna) >= poolmaxs) {
//                break;
//            }
            try {
                //     System.out.println(timeDiff);
                sleep(timeDiff);
            } catch (InterruptedException e) {
                LogUtil.error("连接管理线程中断: " + e.getMessage(), e);
            }
        }


    }


}
