package com.bendb.thrifty;

public abstract class TStruct<S extends TStruct<S, B>, B extends TStruct.Builder<S, B>> {

    public abstract static class Builder<S extends TStruct<S, B>, B extends Builder<S, B>> {
        public Builder() {
            // nothing
        }

        public abstract S build();
    }
}
