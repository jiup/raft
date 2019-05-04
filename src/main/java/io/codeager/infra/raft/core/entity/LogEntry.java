package io.codeager.infra.raft.core.entity;


import com.google.protobuf.StringValue;

public class LogEntry {
    private int index;
    private int term;
    private String key;
    private String value;
    private int version;

    private LogEntry(int index, int term, String key, String value, int version) {
        this.index = index;
        this.term = term;
        this.key = key;
        this.value = value;
        this.version = version;
    }

    public static LogEntry of(int index, int term, String key, String value, int version) {
        return new LogEntry(index, term, key, value, version);
    }

    public static LogEntry of(io.grpc.vote.LogEntry logEntry) {
        return LogEntry.of(logEntry.getIndex(), logEntry.getTerm(), logEntry.getKey(), logEntry.getValue().getValue(), logEntry.getVersion());
    }

    public io.grpc.vote.LogEntry toRpcEntry() {
        io.grpc.vote.LogEntry.Builder builder = io.grpc.vote.LogEntry.newBuilder().setIndex(this.index)
                .setTerm(this.term)
                .setKey(this.key)
                .setVersion(this.version);
        return this.value != null ? builder.setValue(StringValue.of(this.value)).build() : builder.build();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
