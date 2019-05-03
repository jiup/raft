package io.codeager.infra.raft.core.entity;


import com.google.protobuf.StringValue;
import io.grpc.vote.LogEntry;

public class LogEntity {
    private int index;
    private int term;
    private String key;
    private String value;
    private int version;

    private LogEntity(int index, int term, String key, String value, int version) {
        this.index = index;
        this.term = term;
        this.key = key;
        this.value = value;
        this.version = version;
    }

    public static LogEntity of(int index, int term, String key, String value, int version) {
        return new LogEntity(index, term, key, value, version);
    }

    public static LogEntity of(LogEntry logEntry) {
        return LogEntity.of(logEntry.getIndex(), logEntry.getTerm(), logEntry.getKey(), logEntry.getValue().getValue(), logEntry.getVersion());
    }

    public LogEntry toRPCEntry() {
        return LogEntry.newBuilder().setIndex(this.index)
                .setTerm(this.term)
                .setKey(this.key)
                .setValue(StringValue.of(this.value))
                .setVersion(this.version).build();
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
