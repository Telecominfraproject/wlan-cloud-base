package com.telecominfraproject.wlan.core.server.cassandra;

import org.springframework.lang.Nullable;

import com.datastax.oss.driver.api.core.cql.Row;

/**
 * @author dtop
 *
 * @param <T>
 */
public interface RowMapper<T> {

	/**
	 * Implementations must implement this method to map each row of data
	 * in the ResultSet. This method should not traverse the ResultSet; it is only supposed to map values of the current row.
	 * @param row the Row to map 
	 * @return the result object for the current row (may be {@code null})
	 */
	@Nullable
	T mapRow(Row row);

}
