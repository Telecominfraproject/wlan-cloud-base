package com.whizcontrol.core.model.wrappers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import com.whizcontrol.core.model.json.BaseJsonModel;

public class CurrentAndPreviousState<T extends Enum<T>> extends BaseJsonModel {
    private static final long serialVersionUID = -5487444646623716492L;
    private T currentState;
    private T previousState;

    protected CurrentAndPreviousState() {
        // nothing
    }

    public CurrentAndPreviousState(T currentState) {
        this.previousState = null;
        this.currentState = currentState;
    }

    public T getCurrentState() {
        return currentState;
    }

    private void setCurrentState(T currentState) {
        this.currentState = currentState;
    }

    public T getPreviousState() {
        return previousState;
    }

    private void setPreviousState(T previousState) {
        this.previousState = previousState;
    }

    @JsonIgnore
    public T setState(T newState) {
        if (!Objects.equals(newState, currentState)) {
            this.previousState = currentState;
            this.currentState = newState;
        }

        return this.currentState;
    }

    @JsonIgnore
    public T setPreviousState() {
        this.currentState = this.previousState;
        this.previousState = null;

        return this.currentState;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((currentState == null) ? 0 : currentState.hashCode());
        result = prime * result + ((previousState == null) ? 0 : previousState.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CurrentAndPreviousState other = (CurrentAndPreviousState) obj;
        if (currentState == null) {
            if (other.currentState != null) {
                return false;
            }
        } else if (!currentState.equals(other.currentState)) {
            return false;
        }
        if (previousState == null) {
            if (other.previousState != null) {
                return false;
            }
        } else if (!previousState.equals(other.previousState)) {
            return false;
        }
        return true;
    }
}
