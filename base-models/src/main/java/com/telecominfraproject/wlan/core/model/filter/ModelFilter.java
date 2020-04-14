/**
 * 
 */
package com.telecominfraproject.wlan.core.model.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author ekeddy
 *
 */
@SuppressWarnings("serial")
public abstract class ModelFilter<T> extends BaseJsonModel {
    private static final Logger LOG = LoggerFactory.getLogger(ModelFilter.class);

    private static final char[] WHITE_SPACES={' ',' ',' ',' ',' ',' ',' ',' '};

    /**
     * Applies the filter to the supplied data.
     * @param data
     * @return true if data satisfies filter; otherwise returns false
     */
    public boolean apply(T data) {
        return applyImpl(data,0);
    }
    
    private boolean applyImpl(T data, int level) {
        if(LOG.isTraceEnabled() && level == 0) {
            LOG.trace("Evaluating data: {}",data);
        }
        boolean ret = test(data,level+1);
        if(LOG.isTraceEnabled()) {
            String indent = String.copyValueOf(WHITE_SPACES, 0, level*2);
            LOG.trace("{}{} returns {}",indent,this.toTestDisplayString(),ret);
        }
        return ret;
    }

    protected abstract String toTestDisplayString();

    
    /**
     * Test the supplied data to see if it matches the filter.
     * @param data - object under test
     * @return true if data satisfies filter; otherwise returns false
     */
    protected abstract boolean test(T data, int level);

    /**
     * A filter that returns the logical opposite of the supplied child filter.
     */
    public static class Not<T> extends ModelFilter<T> {

        private ModelFilter<T> filter;

        protected Not() {}

        public Not(ModelFilter<T> filter) {
            this.filter = filter;
        }

        public ModelFilter<T> getFilter() {
            return this.filter;
        }

        protected void setFilter(ModelFilter<T> filter) {
            this.filter = filter;
        }

        @Override
        protected boolean test(T model, int level) {
            return !filter.applyImpl(model, level);
        }  

        public static <T> ModelFilter<T> of(ModelFilter<T> filter) {
            return new ModelFilter.Not<>(filter);
        }

        @Override
        protected String toTestDisplayString() {
            return this.getClass().getSimpleName();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(filter);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof Not)) {
                return false;
            }
            @SuppressWarnings("unchecked")
			Not<T> other = (Not<T>) obj;
            return Objects.equals(filter, other.filter);
        }
    }

    /**
     * A filter that performs boolean AND of all the child filters.
     */
    public static class And<T> extends ModelFilter<T> {
        private List<? extends ModelFilter<T>> filters;
        protected And() {}
        public And(ModelFilter<T>[] filters) {
            this.filters = Arrays.asList(filters);
        }

        public And(List<ModelFilter<T>> filters) {
            this.filters = filters;
        }

        @Override
        protected boolean test(T data, int level) {
            for(ModelFilter<T> f: filters) {
                if(f!=null && !f.applyImpl(data, level)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(filters);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof And)) {
                return false;
            }
            @SuppressWarnings("unchecked")
			And<T> other = (And<T>) obj;
            return Objects.equals(filters, other.filters);
        }

        public static <T> ModelFilter<T> of(ModelFilter<T> filter1, ModelFilter<T> filter2) {
            return new ModelFilter.And<T>(Arrays.asList(filter1,filter2));
        }

        public static <T> ModelFilter<T> of(ModelFilter<T> filter1, ModelFilter<T> filter2, ModelFilter<T> filter3) {
            return new ModelFilter.And<T>(Arrays.asList(filter1,filter2,filter3));
        }

        public static <T> ModelFilter<T> of(List<ModelFilter<T>> filters) {
            return new ModelFilter.And<T>(filters);
        }
       
        @Override
        protected String toTestDisplayString() {
            return this.getClass().getSimpleName();
        }

        public List<? extends ModelFilter<T>> getFilters() {
            return filters;
        }

        protected void setFilters(List<? extends ModelFilter<T>> filters) {
            this.filters = filters;
        }
    }

    /**
     * A filter that performs boolean OR of all the child filters.
     */
    public static class Or<T> extends ModelFilter<T> {
        private List<? extends ModelFilter<T>> filters;
        protected Or() {}
        public Or(ModelFilter<T>[] filters) {
            this.filters = Arrays.asList(filters);
        }

        public Or(List<? extends ModelFilter<T>> filters) {
            this.filters = filters;
        }

        @Override
        protected boolean test(T data, int level) {
            for(ModelFilter<T> f: filters) {
                if(f != null && f.applyImpl(data, level)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(filters);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof Or)) {
                return false;
            }
            @SuppressWarnings("unchecked")
			Or<T> other = (Or<T>) obj;
            return Objects.equals(filters, other.filters);
        }

        public static <T> ModelFilter<T> of(List<ModelFilter<T>> filters) {
            return new ModelFilter.Or<>(filters);
        }

        public static <T> ModelFilter<T> of(ModelFilter<T> filter1, ModelFilter<T> filter2) {
            return new ModelFilter.Or<>(Arrays.asList(filter1,filter2));
        }

        @Override
        protected String toTestDisplayString() {
            return this.getClass().getSimpleName();
        }

        public List<? extends ModelFilter<T>> getFilters() {
            return this.filters;
        }

        protected void setFilters(List<? extends ModelFilter<T>> filters) {
            this.filters = filters;
        }
    }

    public static boolean hasFilter(ModelFilter<?> filter, Class<? extends ModelFilter<?>> filterClass) {
        if(filter == null) {
            return false;
        }
        
        if(filter.getClass().equals(filterClass)) {
            return true;
        }
        else if(filter instanceof ModelFilter.And) {
            for(ModelFilter<?> subFilter : ((ModelFilter.And<?>)filter).filters) {
                if(subFilter != null && hasFilter(subFilter,filterClass)) {
                    return true;
                }
            }
        }
        else if(filter instanceof ModelFilter.Or) {
            for(ModelFilter<?> subFilter : ((ModelFilter.Or<?>)filter).filters) {
                if(subFilter != null && hasFilter(subFilter,filterClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasFilter(ModelFilter<?> filter, ModelFilter<?> findFilter) {
        if(filter == null) {
            return false;
        }
        if(filter instanceof ModelFilter.And) {
            for(ModelFilter<?> subFilter : ((ModelFilter.And<?>)filter).filters) {
                if(subFilter != null && hasFilter(subFilter,findFilter)) {
                    return true;
                }
            }
        }
        else if(filter instanceof ModelFilter.Or) {
            for(ModelFilter<?> subFilter : ((ModelFilter.Or<?>)filter).filters) {
                if(subFilter != null && hasFilter(subFilter,findFilter)) {
                    return true;
                }
            }
        }
        else if(filter.equals(findFilter)) {
            return true;
        }

        return false;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (getClass().hashCode());
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
        if (!Objects.equals(getClass(), obj.getClass())) {
            return false;
        }
        return true;
    }
}
