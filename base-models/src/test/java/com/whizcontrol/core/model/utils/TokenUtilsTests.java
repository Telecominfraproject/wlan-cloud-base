/**
 * 
 */
package com.whizcontrol.core.model.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * @author yongli
 *
 */
public class TokenUtilsTests {

    public static class TestData extends BaseJsonModel {
        private static final long serialVersionUID = 1543240904806961790L;
        private long currentTime;

        public TestData() {
        }

        public TestData(long currentTime) {
            this.setCurrentTime(currentTime);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof TestData)) {
                return false;
            }
            TestData other = (TestData) obj;
            if (this.currentTime != other.currentTime) {
                return false;
            }
            return true;
        }

        public long getCurrentTime() {
            return currentTime;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (this.currentTime ^ (this.currentTime >>> 32));
            return result;
        }

        public void setCurrentTime(long currentTime) {
            this.currentTime = currentTime;
        }
    }

    @Test
    public void testBase62() {
        TestData data = new TestData(System.currentTimeMillis());
        String token = TokenUtils.encodeToken(data, TokenUtils.PASSWORD, TokenEncoder.Base62TokenEncoder);
        TestData decoded = TokenUtils.decodeToken(token, TestData.class, TokenUtils.PASSWORD,
                TokenEncoder.Base62TokenEncoder);
        assertEquals(data, decoded);
    }
}
