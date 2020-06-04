/*
 *
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2017 MariaDB Ab.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to Monty Program Ab info@montyprogram.com.
 *
 * This particular MariaDB Client for Java file is work
 * derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
 * the following copyright and notice provisions:
 *
 * Copyright (c) 2009-2011, Marcus Eriksson
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of the driver nor the names of its contributors may not be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.mariadb.jdbc;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class StatementTest extends BaseTest {

    private static final int ER_BAD_FIELD_ERROR = 1054;
    private static final int ER_NON_INSERTABLE_TABLE = 1471;
    private static final int ER_NO_SUCH_TABLE = 1146;
    private static final int ER_CMD_NOT_PERMIT = 1148;
    private static final int ER_NONUPDATEABLE_COLUMN = 1348;
    private static final int ER_PARSE_ERROR = 1064;
    private static final int ER_NO_PARTITION_FOR_GIVEN_VALUE = 1526;
    private static final int ER_LOAD_DATA_INVALID_COLUMN = 1611;
    private static final int ER_ADD_PARTITION_NO_NEW_PARTITION = 1514;
    private static final String ER_BAD_FIELD_ERROR_STATE = "42S22";
    private static final String ER_NON_INSERTABLE_TABLE_STATE = "HY000";
    private static final String ER_NO_SUCH_TABLE_STATE = "42S02";
    private static final String ER_NONUPDATEABLE_COLUMN_STATE = "HY000";
    private static final String ER_PARSE_ERROR_STATE = "42000";
    private static final String ER_NO_PARTITION_FOR_GIVEN_VALUE_STATE = "HY000";
    private static final String ER_LOAD_DATA_INVALID_COLUMN_STATE = "HY000";
    private static final String ER_ADD_PARTITION_NO_NEW_PARTITION_STATE = "HY000";


    /**
     * Initializing tables.
     *
     * @throws SQLException exception
     */
    @BeforeClass()
    public static void initClass() throws SQLException {
        createTable("vendor_code_test", "id int not null primary key auto_increment, test boolean");
        createTable("vendor_code_test2", "a INT", "PARTITION BY KEY (a) (PARTITION x0, PARTITION x1)");
        createTable("vendor_code_test3", "a INT", "PARTITION BY LIST(a) (PARTITION p0 VALUES IN (1))");
        createTable("StatementTestt1", "c1 INT, c2 VARCHAR(255)");


    }


    @Test
    public void wrapperTest() throws SQLException {
        Statement statement = sharedConnection.createStatement();
        assertTrue(statement.isWrapperFor(Statement.class));
        assertFalse(statement.isWrapperFor(SQLException.class));
        assertThat(statement.unwrap(Statement.class), equalTo(statement));
        try {
            statement.unwrap(SQLException.class);
            fail("MariaDbStatement class unwrapped as SQLException class");
        } catch (SQLException sqle) {
            //normal exception
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Conj-90.
     *
     * @throws SQLException exception
     */
    @Test
    public void reexecuteStatementTest() throws SQLException {
        Connection connection = null;
        try {
            connection = setConnection("&allowMultiQueries=true");
            PreparedStatement stmt = null;
            try {
                stmt = connection.prepareStatement("SELECT 1");
                stmt.setFetchSize(Integer.MIN_VALUE);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                rs = stmt.executeQuery();
            } finally {
                if (stmt != null) stmt.close();
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test(expected = SQLException.class)
    public void afterConnectionClosedTest() throws SQLException {
        Connection conn2 = DriverManager.getConnection("jdbc:mariadb://localhost:3306/test?user=root");
        Statement st1 = conn2.createStatement();
        st1.close();
        conn2.close();
        Statement st2 = conn2.createStatement();
        assertTrue(false);
        st2.close();
    }

    @Test
    public void testColumnsDoNotExist() throws SQLException {

        try {
            sharedConnection.createStatement().executeQuery(
                    "select * from vendor_code_test where crazy_column_that_does_not_exist = 1");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            assertEquals(ER_BAD_FIELD_ERROR, sqlException.getErrorCode());
            assertEquals(ER_BAD_FIELD_ERROR_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testNonInsertableTable() throws SQLException {
        Statement statement = sharedConnection.createStatement();
        statement.execute("create or replace view vendor_code_test_view as select id as id1, id as id2, test "
                + "from vendor_code_test");

        try {
            statement.executeQuery("insert into vendor_code_test_view VALUES (null, null, true)");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            assertEquals(ER_NON_INSERTABLE_TABLE, sqlException.getErrorCode());
            assertEquals(ER_NON_INSERTABLE_TABLE_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testNoSuchTable() throws SQLException, UnsupportedEncodingException {
        Statement statement = sharedConnection.createStatement();
        statement.execute("drop table if exists vendor_code_test_");
        try {
            statement.execute("SELECT * FROM vendor_code_test_");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            if (sqlException.getErrorCode() != ER_NO_SUCH_TABLE && sqlException.getErrorCode() != ER_CMD_NOT_PERMIT) {
                fail("Wrong error code message");
            }
            assertEquals(ER_NO_SUCH_TABLE_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testNoSuchTableBatchUpdate() throws SQLException, UnsupportedEncodingException {
        Statement statement = sharedConnection.createStatement();
        statement.execute("drop table if exists vendor_code_test_");
        statement.addBatch("INSERT INTO vendor_code_test_ VALUES('dummyValue')");
        try {
            statement.executeBatch();
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            if (sqlException.getErrorCode() != ER_NO_SUCH_TABLE && sqlException.getErrorCode() != ER_CMD_NOT_PERMIT) {
                fail("Wrong error code message");
            }
            assertEquals(ER_NO_SUCH_TABLE_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testNonUpdateableColumn() throws SQLException {
        Statement statement = sharedConnection.createStatement();
        statement.execute("create or replace view vendor_code_test_view as select *,"
                + " 1 as derived_column_that_does_no_exist from vendor_code_test");

        try {
            statement.executeQuery("UPDATE vendor_code_test_view SET derived_column_that_does_no_exist = 1");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            assertEquals(ER_NONUPDATEABLE_COLUMN, sqlException.getErrorCode());
            assertEquals(ER_NONUPDATEABLE_COLUMN_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testParseErrorAddPartitionNoNewPartition() throws SQLException {
        Statement statement = sharedConnection.createStatement();
        try {
            statement.execute("totally_not_a_sql_command_this_cannot_be_parsed");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            assertEquals(ER_PARSE_ERROR, sqlException.getErrorCode());
            assertEquals(ER_PARSE_ERROR_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testAddPartitionNoNewPartition() throws SQLException {
        Statement statement = sharedConnection.createStatement();
        try {
            statement.execute("ALTER TABLE vendor_code_test2 ADD PARTITION PARTITIONS 0");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            assertEquals(ER_ADD_PARTITION_NO_NEW_PARTITION, sqlException.getErrorCode());
            assertEquals(ER_ADD_PARTITION_NO_NEW_PARTITION_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testNoPartitionForGivenValue() throws SQLException {
        Statement statement = sharedConnection.createStatement();
        statement.execute("INSERT INTO vendor_code_test3 VALUES (1)");
        try {
            statement.execute("INSERT INTO vendor_code_test3 VALUES (2)");
            fail("The above statement should result in an exception");
        } catch (SQLException sqlException) {
            assertEquals(ER_NO_PARTITION_FOR_GIVEN_VALUE, sqlException.getErrorCode());
            assertEquals(ER_NO_PARTITION_FOR_GIVEN_VALUE_STATE, sqlException.getSQLState());
        }
    }

    @Test
    public void testLoadDataInvalidColumn() throws SQLException, UnsupportedEncodingException {
        Statement statement = sharedConnection.createStatement();
        try {
            statement.execute("drop view if exists v2");
        } catch (SQLException e) {
            //if view doesn't exist, and mode throw warning as error
        }
        statement.execute("CREATE VIEW v2 AS SELECT 1 + 2 AS c0, c1, c2 FROM StatementTestt1;");
        try {
            MariaDbStatement mysqlStatement;
            if (statement.isWrapperFor(MariaDbStatement.class)) {
                mysqlStatement = statement.unwrap(MariaDbStatement.class);
            } else {
                throw new RuntimeException("Mariadb JDBC adaptor must be used");
            }
            try {
                String data = "\"1\", \"string1\"\n"
                        + "\"2\", \"string2\"\n"
                        + "\"3\", \"string3\"\n";
                ByteArrayInputStream loadDataInfileFile = new ByteArrayInputStream(data.getBytes("utf-8"));
                mysqlStatement.setLocalInfileInputStream(loadDataInfileFile);
                mysqlStatement.executeUpdate("LOAD DATA LOCAL INFILE 'dummyFileName' INTO TABLE v2 "
                        + "FIELDS ESCAPED BY '\\\\' "
                        + "TERMINATED BY ',' "
                        + "ENCLOSED BY '\"'"
                        + "LINES TERMINATED BY '\n' (c0, c2)");
                fail("The above statement should result in an exception");
            } catch (SQLException sqlException) {
                if (sqlException.getErrorCode() != ER_LOAD_DATA_INVALID_COLUMN && sqlException.getErrorCode() != ER_NONUPDATEABLE_COLUMN) {
                    fail();
                }
                assertEquals(ER_LOAD_DATA_INVALID_COLUMN_STATE, sqlException.getSQLState());
            }
        } finally {
            try {
                statement.execute("drop view if exists v2");
            } catch (SQLException e) {
                //if view doesn't exist, and mode throw warning as error
            }
        }
    }

    @Test(timeout = 10000)
    public void statementClose() throws SQLException {
        Assume.assumeTrue(sharedOptions().socketTimeout == null);
        Properties infos = new Properties();
        infos.put("socketTimeout", 1000);
        Connection connection = null;
        try {
            connection = createProxyConnection(infos);
            Statement statement = connection.createStatement();
            Statement otherStatement = null;
            try {
                otherStatement = connection.createStatement();
                stopProxy();
                otherStatement.execute("SELECT 1");
            } catch (SQLException e) {
                assertTrue(otherStatement.isClosed());
                assertTrue(connection.isClosed());
                try {
                    statement.execute("SELECT 1");
                } catch (SQLException ee) {
                    assertTrue(statement.isClosed());
                    assertEquals("must be an SQLState 08000 exception", "08000", ee.getSQLState());
                }
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void closeOnCompletion() throws SQLException {
        Statement st = sharedConnection.createStatement();
        MariaDbStatement statement = (MariaDbStatement) st;
        assertFalse(statement.isCloseOnCompletion());
        ResultSet rs = statement.executeQuery("SELECT 1");
        statement.closeOnCompletion();
        assertTrue(statement.isCloseOnCompletion());
        assertFalse(statement.isClosed());
        rs.close();
        assertTrue(statement.isClosed());
    }

    @Test
    public void testFractionalTimeBatch() throws SQLException {
        Assume.assumeTrue(doPrecisionTest);

        createTable("testFractionalTimeBatch", "tt TIMESTAMP(6)");
        Timestamp currTime = new Timestamp(System.currentTimeMillis());
        PreparedStatement preparedStatement = sharedConnection.prepareStatement(
                "INSERT INTO testFractionalTimeBatch (tt) values (?)");
        for (int i = 0; i < 2; i++) {
            preparedStatement.setTimestamp(1, currTime);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();

        Statement statement = sharedConnection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * from testFractionalTimeBatch");
        assertTrue(resultSet.next());
        assertEquals(resultSet.getTimestamp(1).getNanos(), currTime.getNanos());
    }
}