package cc.ruixing.framework.tarn.aop;

/**
 * Created by dfr on 14-9-28.
 */
public class ControllerMethodParamClass {
    private Class typeas;
    private String annName;
    private Class annClass;
    private boolean isAnn;

    public ControllerMethodParamClass(Class typeas, String annName, Class annClass, boolean isAnn) {
        this.typeas = typeas;
        this.annName = annName;
        this.annClass = annClass;
        this.isAnn = isAnn;
    }

    public ControllerMethodParamClass(Class typeas) {
        this.typeas = typeas;
    }

    public Class getTypeas() {
        return typeas;
    }

    public void setTypeas(Class typeas) {
        this.typeas = typeas;
    }

    public String getAnnName() {
        return annName;
    }

    public void setAnnName(String annName) {
        this.annName = annName;
    }

    public Class getAnnClass() {
        return annClass;
    }

    public void setAnnClass(Class annClass) {
        this.annClass = annClass;
    }

    public boolean isAnn() {
        return isAnn;
    }

    public void setAnn(boolean isAnn) {
        this.isAnn = isAnn;
    }
}
