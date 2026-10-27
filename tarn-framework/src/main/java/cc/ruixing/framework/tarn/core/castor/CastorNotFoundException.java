package cc.ruixing.framework.tarn.core.castor;



public class CastorNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public CastorNotFoundException(String message) {
        super(message);
    }

    public CastorNotFoundException(Throwable t) {
        super(t);
    }
}

