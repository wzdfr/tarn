package cc.ruixing.framework.tarn.collection;

public class RunMode {

    private boolean jarStatus = false;

    private static RunMode instance = new RunMode();

    public void updateJarStatus(boolean status) {
        this.jarStatus = status;
    }

    public boolean isJarStatus() {
        return jarStatus;
    }

    private RunMode() {

    }

    public static RunMode getInstance() {
        return instance;
    }


}
