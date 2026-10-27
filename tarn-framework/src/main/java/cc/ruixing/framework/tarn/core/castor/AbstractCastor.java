package cc.ruixing.framework.tarn.core.castor;

public abstract class AbstractCastor
        implements ICastor {
    public String getID() {
        return getClass().getName();
    }

    public String getName() {
        return getClass().getName();
    }
}

