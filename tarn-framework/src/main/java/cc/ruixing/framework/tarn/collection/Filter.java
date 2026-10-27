package cc.ruixing.framework.tarn.collection;

public abstract class Filter<T> {
    protected Object param;

    public Filter() {
    }

    public Filter(Object param) {
        this.param = param;
    }

    public abstract boolean filter(T paramT);
}

