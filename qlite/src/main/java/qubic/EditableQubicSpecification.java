package qubic;

public class EditableQubicSpecification extends QubicSpecification {

    private int executionStartSecondsInFuture = 300;

    public EditableQubicSpecification() {
        executionStartUnix = 0; // setter would cause exception
        setHashPeriodDuration(20);
        setResultPeriodDuration(10);
        setRuntimeLimit(10);
    }

    public EditableQubicSpecification(QubicSpecification origin) {
        super(origin);
    }

    public void setExecutionStartUnix(int executionStartUnix) {
        if(executionStartUnix < currentUnixTimestamp())
            throw new IllegalStateException("executionStartUnix must be greater than current unix timestamp (="+currentUnixTimestamp()+")");
        executionStartSecondsInFuture = 0;
        this.executionStartUnix = executionStartUnix;
    }

    public void setExecutionStartToSecondsInFuture(int secondsInFuture) {
        if(secondsInFuture <= 0)
            throw new IllegalStateException("secondsInFuture must be greater than 0");
        executionStartSecondsInFuture = secondsInFuture;
        executionStartUnix = 0;
    }

    @Override
    public int getExecutionStartUnix() {
        if(executionStartUnix == 0)
            return currentUnixTimestamp() + executionStartSecondsInFuture;
        return executionStartUnix;
    }

    public void setHashPeriodDuration(int hashPeriodDuration) {
        this.hashPeriodDuration = hashPeriodDuration;
    }

    public void setResultPeriodDuration(int resultPeriodDuration) {
        this.resultPeriodDuration = resultPeriodDuration;
    }

    public void setRuntimeLimit(int runTimeLimit) {
        this.runtimeLimit = runTimeLimit;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
