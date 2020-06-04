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
import org.mariadb.jdbc.internal.util.DefaultOptions;
import org.mariadb.jdbc.internal.util.constant.HaMode;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


public class DriverTest extends BaseTest {

    /**
     * Tables initialisation.
     *
     * @throws SQLException exception
     */
    @BeforeClass()
    public static void initClass() throws SQLException {
        createTable("tt1", "id int , name varchar(20)");
        createTable("tt2", "id int , name varchar(20)");
        createTable("Drivert2", "id int not null primary key auto_increment, test varchar(10)");
        createTable("utente", "id int not null primary key auto_increment, test varchar(10)");

        createTable("Drivert3", "id int not null primary key auto_increment, test varchar(10)");
        createTable("Drivert30", "id int not null primary key auto_increment, test varchar(20)", "engine=innodb");
        createTable("Drivert4", "id int not null primary key auto_increment, test varchar(20)", "engine=innodb");
        createTable("test_float", "id int not null primary key auto_increment, a float");
        createTable("test_big_autoinc2", "id int not null primary key auto_increment, test varchar(10)");
        createTable("test_big_update", "id int primary key not null, updateme int");
        createTable("sharedConnection", "id int");
        createTable("extest", "id int not null primary key");
        createTable("commentPreparedStatements", "id int not null primary key auto_increment, a varchar(10)");
        createTable("quotesPreparedStatements", "id int not null primary key auto_increment, a varchar(10) , "
                + "b varchar(10)");
        createTable("ressetpos", "i int not null primary key", "engine=innodb");
        createTable("streamingtest", "val varchar(20)");
        createTable("testBlob2", "a blob");
        createTable("testString2", "a varchar(10)");
        createTable("testBlob2", "a blob");
        createTable("unsignedtest", "a int unsigned");
        createTable("conj25", "a VARCHAR(1024)");
        createTable("DriverTestt1", "id int not null primary key auto_increment, test varchar(20)");
        createTable("DriverTestt2", "id int not null primary key auto_increment, test varchar(20)");
        createTable("DriverTestt3", "id int not null primary key auto_increment, test varchar(20)");
        createTable("DriverTestt4", "id int not null primary key auto_increment, test varchar(20)");
        createTable("DriverTestt5", "id int not null primary key auto_increment, test varchar(20)");
        createProcedure("foo", "() BEGIN SELECT 1; END");
        createTable("conj275", "a VARCHAR(10)");
    }

    @Test
    public void doQuery() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("insert into DriverTestt1 (test) values ('hej1')");
        stmt.execute("insert into DriverTestt1 (test) values ('hej2')");
        stmt.execute("insert into DriverTestt1 (test) values ('hej3')");
        stmt.execute("insert into DriverTestt1 (test) values (null)");
        ResultSet rs = stmt.executeQuery("select * from DriverTestt1");
        for (int i = 1; i < 4; i++) {
            assertTrue(rs.next());
            assertEquals(String.valueOf(i), rs.getString(1));
            assertEquals("hej" + i, rs.getString("test"));
        }
        assertTrue(rs.next());
        assertEquals(null, rs.getString("test"));
    }

    @Test(expected = SQLException.class)
    public void askForBadColumnTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("insert into DriverTestt2 (test) values ('hej1')");
        stmt.execute("insert into DriverTestt2 (test) values ('hej2')");
        stmt.execute("insert into DriverTestt2 (test) values ('hej3')");
        stmt.execute("insert into DriverTestt2 (test) values (null)");
        ResultSet rs = stmt.executeQuery("select * from DriverTestt2");
        if (rs.next()) {
            rs.getInt("non_existing_column");
        } else {
            fail();
        }
    }

    @Test(expected = SQLException.class)
    public void askForBadColumnIndexTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("insert into DriverTestt3 (test) values ('hej1')");
        stmt.execute("insert into DriverTestt3 (test) values ('hej2')");
        stmt.execute("insert into DriverTestt3 (test) values ('hej3')");
        stmt.execute("insert into DriverTestt3 (test) values (null)");
        ResultSet rs = stmt.executeQuery("select * from DriverTestt3");
        assertTrue(rs.next());
        rs.getInt(102);
    }

    @Test
    /* Accessing result set using  table.column */
    public void tableDotColumnInResultSet() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("insert into tt1 values(1, 'one')");
        stmt.execute("insert into tt2 values(1, 'two')");
        ResultSet rs = stmt.executeQuery("select tt1.*, tt2.* from tt1, tt2 where tt1.id = tt2.id");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("tt1.id"));
        assertEquals(1, rs.getInt("tt2.id"));
        assertEquals("one", rs.getString("tt1.name"));
        assertEquals("two", rs.getString("tt2.name"));
    }

    @Test(expected = SQLException.class)
    public void badQuery() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.executeQuery("whraoaooa");
    }

    @Test
    public void preparedTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("insert into DriverTestt4 (test) values ('hej1')");

        String query = "SELECT * FROM DriverTestt4 WHERE test = ? and id = ?";
        PreparedStatement prepStmt = sharedConnection.prepareStatement(query);
        prepStmt.setString(1, "hej1");
        prepStmt.setInt(2, 1);
        ResultSet results = prepStmt.executeQuery();
        String res = "";
        while (results.next()) {
            res = results.getString("test");
        }
        assertEquals("hej1", res);
        assertEquals(2, prepStmt.getParameterMetaData().getParameterCount());
    }

    @Test
    public void parameterMetaDataNotPreparable() throws SQLException {
        Assume.assumeFalse(sharedUsePrepare());
        Statement stmt = sharedConnection.createStatement();
        Map<String, Integer> initValues = loadVariables(stmt);

        //statement that cannot be prepared
        PreparedStatement pstmt = null;
        try {
            pstmt = sharedConnection.prepareStatement("select  TMP.field1 from (select ? from dual) TMP");
            try {
                ParameterMetaData parameterMetaData = pstmt.getParameterMetaData();
                fail();
            } catch (SQLException sqle) {
                assertEquals("42S22", sqle.getSQLState());
                assertTrue(sqle.getMessage().contains("Unknown column"));
            }
        } finally {
            if (pstmt != null) pstmt.close();
        }
        Map<String, Integer> endingValues = loadVariables(stmt);
        assertEquals(initValues.get("Prepared_stmt_count"), endingValues.get("Prepared_stmt_count"));
        assertEquals((Integer) (initValues.get("Com_stmt_prepare") + 1), endingValues.get("Com_stmt_prepare"));
        assertEquals(initValues.get("Com_stmt_close"), endingValues.get("Com_stmt_close"));
    }

    @Test
    public void parameterMetaDataReturnException() throws SQLException {
        Assume.assumeFalse(sharedUsePrepare());
        //statement that cannot be prepared
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = sharedConnection.prepareStatement("selec1t 2 from dual");
            try {
                preparedStatement.getParameterMetaData();
                fail();
            } catch (SQLException sqle) {
                assertEquals("42000", sqle.getSQLState());
                assertTrue(sqle.getMessage().contains("You have an error in your SQL syntax"));
            }
        } finally {
            if (preparedStatement != null) preparedStatement.close();
        }
    }

    private Map<String, Integer> loadVariables(Statement stmt) throws SQLException {
        Map<String, Integer> variables = new HashMap<String, Integer>();
        ResultSet rs = stmt.executeQuery("SHOW SESSION STATUS WHERE Variable_name in ('Prepared_stmt_count','Com_stmt_prepare', 'Com_stmt_close')");
        assertTrue(rs.next());
        variables.put(rs.getString(1), rs.getInt(2));
        assertTrue(rs.next());
        variables.put(rs.getString(1), rs.getInt(2));
        assertTrue(rs.next());
        variables.put(rs.getString(1), rs.getInt(2));
        return variables;
    }

    @Test
    public void parameterMetaDataPreparable() throws SQLException {
        Assume.assumeFalse(sharedUsePrepare());
        Statement stmt = sharedConnection.createStatement();
        Map<String, Integer> initValues = loadVariables(stmt);

        //statement that cannot be prepared
        PreparedStatement pstmt = null;
        try {
            pstmt = sharedConnection.prepareStatement("select  ?");
            ParameterMetaData parameterMetaData = pstmt.getParameterMetaData();
            parameterMetaData.getParameterCount();
        } finally {
            if (pstmt == null) pstmt.close();
        }

        Map<String, Integer> endingValues = loadVariables(stmt);
        assertEquals(initValues.get("Prepared_stmt_count"), endingValues.get("Prepared_stmt_count"));
        assertEquals((Integer) (initValues.get("Com_stmt_prepare") + 1), endingValues.get("Com_stmt_prepare"));
        assertEquals((Integer) (initValues.get("Com_stmt_close") + 1), endingValues.get("Com_stmt_close"));

    }

    @Test
    public void streamingResultSet() throws Exception {
        Statement stmt = sharedConnection.createStatement();
        stmt.setFetchSize(Integer.MIN_VALUE);
        ResultSet rs = stmt.executeQuery("SELECT 1");
        assertTrue(rs.isBeforeFirst());
        try {
            rs.first();
            fail("should not get there");
        } catch (SQLException sqle) {
            assertTrue(sqle.getMessage().toLowerCase().contains("invalid operation"));
        }
    }

    @Test
    public void updateTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();

        stmt.execute("insert into DriverTestt5 (test) values ('hej1')");
        stmt.execute("insert into DriverTestt5 (test) values ('hej2')");
        stmt.execute("insert into DriverTestt5 (test) values ('hej3')");
        stmt.execute("insert into DriverTestt5 (test) values (null)");

        String query = "UPDATE DriverTestt5 SET test = ? where id = ?";
        PreparedStatement prepStmt = sharedConnection.prepareStatement(query);
        prepStmt.setString(1, "updated");
        prepStmt.setInt(2, 3);
        int updateCount = prepStmt.executeUpdate();
        assertEquals(1, updateCount);
        String query2 = "SELECT * FROM DriverTestt5 WHERE id=?";
        prepStmt = sharedConnection.prepareStatement(query2);
        prepStmt.setInt(1, 3);
        ResultSet results = prepStmt.executeQuery();
        String result = "";
        while (results.next()) {
            result = results.getString("test");
        }
        assertEquals("updated", result);
    }

    @Test
    public void ralfTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        for (int i = 0; i < 10; i++) {
            stmt.execute("INSERT INTO Drivert2 (test) VALUES ('aßa" + i + "')");
        }
        PreparedStatement ps = sharedConnection.prepareStatement("SELECT * FROM Drivert2 where test like'%ß%' limit ?");
        ps.setInt(1, 5);
        ps.addBatch();
        ResultSet rs = ps.executeQuery();
        int result = 0;
        while (rs.next()) {
            result++;
        }
        assertEquals(result, 5);
    }

    @Test
    public void autoIncTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("INSERT INTO Drivert3 (test) VALUES ('aa')", Statement.RETURN_GENERATED_KEYS);
        ResultSet rs = stmt.getGeneratedKeys();
        assertTrue(rs.next());

        assertEquals(1, rs.getInt(1));
        assertEquals(1, rs.getInt("insert_id"));

        stmt.execute("INSERT INTO Drivert3 (test) VALUES ('aa')", Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        assertTrue(rs.next());

        assertEquals(2, rs.getInt(1));
        assertEquals(2, rs.getInt("insert_id"));
        
        
        /* multi-row inserts */
        stmt.execute("INSERT INTO Drivert3 (test) VALUES ('bb'),('cc'),('dd')", Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));

        requireMinimumVersion(5, 0);
        /* non-standard autoIncrementIncrement */

        Connection connection = null;
        try {
            connection = setConnection("&sessionVariables=auto_increment_increment=2&allowMultiQueries=true");
            stmt = connection.createStatement();
            stmt.execute("INSERT INTO Drivert3 (test) values ('bb'),('cc');INSERT INTO Drivert3 (test) values ('dd'),('ee')",
                    Statement.RETURN_GENERATED_KEYS);

            rs = stmt.getGeneratedKeys();

            assertTrue(rs.next());
            assertEquals(7, rs.getInt(1));
            assertTrue(rs.next());
            assertEquals(9, rs.getInt(1));
            assertFalse(rs.next());

            stmt.getMoreResults();

            rs = stmt.getGeneratedKeys();

            assertTrue(rs.next());
            assertEquals(11, rs.getInt(1));
            assertTrue(rs.next());
            assertEquals(13, rs.getInt(1));
            assertFalse(rs.next());
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void autoInc2Test() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("ALTER TABLE `utente` AUTO_INCREMENT=1", Statement.RETURN_GENERATED_KEYS);
    }


    @Test
    public void transactionTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        sharedConnection.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO Drivert30 (test) VALUES ('heja')");
        stmt.executeUpdate("INSERT INTO Drivert30 (test) VALUES ('japp')");
        sharedConnection.commit();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Drivert30");
        assertEquals(true, rs.next());
        assertEquals("heja", rs.getString("test"));
        assertEquals(true, rs.next());
        assertEquals("japp", rs.getString("test"));
        assertEquals(false, rs.next());
        stmt.executeUpdate("INSERT INTO Drivert30 (test) VALUES ('rollmeback')", Statement.RETURN_GENERATED_KEYS);
        ResultSet rsGen = stmt.getGeneratedKeys();
        rsGen.next();
        assertEquals(3, rsGen.getInt(1));
        sharedConnection.rollback();
        rs = stmt.executeQuery("SELECT * FROM Drivert30 WHERE id=3");
        assertEquals(false, rs.next());
        sharedConnection.setAutoCommit(true);
    }

    @Test
    public void savepointTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        sharedConnection.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO Drivert4 (test) values('hej1')");
        stmt.executeUpdate("INSERT INTO Drivert4 (test) values('hej2')");
        Savepoint savepoint = sharedConnection.setSavepoint("yep");
        stmt.executeUpdate("INSERT INTO Drivert4 (test)  values('hej3')");
        stmt.executeUpdate("INSERT INTO Drivert4 (test) values('hej4')");
        sharedConnection.rollback(savepoint);
        stmt.executeUpdate("INSERT INTO Drivert4 (test) values('hej5')");
        stmt.executeUpdate("INSERT INTO Drivert4 (test) values('hej6')");
        sharedConnection.commit();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Drivert4");
        assertEquals(true, rs.next());
        assertEquals("hej1", rs.getString(2));
        assertEquals(true, rs.next());
        assertEquals("hej2", rs.getString(2));
        assertEquals(true, rs.next());
        assertEquals("hej5", rs.getString(2));
        assertEquals(true, rs.next());
        assertEquals("hej6", rs.getString(2));
        assertEquals(false, rs.next());
        sharedConnection.setAutoCommit(true);
    }

    @Test
    public void isolationLevel() throws SQLException {
        Connection connection = null;
        try {
            connection = setConnection();
            int[] levels = new int[]{
                    Connection.TRANSACTION_READ_UNCOMMITTED,
                    Connection.TRANSACTION_READ_COMMITTED,
                    Connection.TRANSACTION_SERIALIZABLE,
                    Connection.TRANSACTION_REPEATABLE_READ
            };
            for (int level : levels) {
                connection.setTransactionIsolation(level);
                assertEquals(level, connection.getTransactionIsolation());
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void isValidTest() throws SQLException {
        assertEquals(true, sharedConnection.isValid(0));
    }

    @Test
    public void testConnectorJurl() throws SQLException {
        UrlParser url = UrlParser.parse("jdbc:mariadb://localhost/test");
        assertEquals("localhost", url.getHostAddresses().get(0).host);
        assertEquals("test", url.getDatabase());
        assertEquals(3306, url.getHostAddresses().get(0).port);

        url = UrlParser.parse("jdbc:mariadb://localhost:3307/test");
        assertEquals("localhost", url.getHostAddresses().get(0).host);
        assertEquals("test", url.getDatabase());
        assertEquals(3307, url.getHostAddresses().get(0).port);

    }


    @Test
    public void testAliasReplication() throws SQLException {
        UrlParser url = UrlParser.parse("jdbc:mysql:replication://localhost/test");
        UrlParser url2 = UrlParser.parse("jdbc:mariadb:replication://localhost/test");
        assertEquals(url.getDatabase(), url2.getDatabase());
        assertEquals(url.getOptions(), url2.getOptions());
        assertEquals(url.getHostAddresses(), url2.getHostAddresses());
        assertEquals(url.getHaMode(), url2.getHaMode());

    }

    @Test
    public void testAliasDataSource() throws SQLException {
        ArrayList<HostAddress> hostAddresses = new ArrayList<HostAddress>();
        hostAddresses.add(new HostAddress(hostname, port));
        UrlParser urlParser = new UrlParser(database, hostAddresses, DefaultOptions.defaultValues(HaMode.NONE), HaMode.NONE);
        UrlParser urlParser2 = new UrlParser(database, hostAddresses, DefaultOptions.defaultValues(HaMode.NONE), HaMode.NONE);

        urlParser.parseUrl("jdbc:mysql:replication://localhost/test");
        urlParser2.parseUrl("jdbc:mariadb:replication://localhost/test");
        assertEquals(urlParser.getDatabase(), urlParser2.getDatabase());
        assertEquals(urlParser.getOptions(), urlParser2.getOptions());
        assertEquals(urlParser.getHostAddresses(), urlParser2.getHostAddresses());
        assertEquals(urlParser.getHaMode(), urlParser2.getHaMode());
    }


    @Test
    public void testEscapes() throws SQLException {
        String query = "select ?";
        PreparedStatement stmt = sharedConnection.prepareStatement(query);
        stmt.setString(1, "hej\"");
        ResultSet rs = stmt.executeQuery();
        assertEquals(true, rs.next());
        assertEquals(rs.getString(1), "hej\"");
    }

    @Test
    public void testPreparedWithNull() throws SQLException {
        String query = "select ? as test";
        PreparedStatement pstmt = sharedConnection.prepareStatement(query);
        pstmt.setNull(1, 1);
        ResultSet rs = pstmt.executeQuery();
        assertEquals(true, rs.next());
        assertEquals(null, rs.getString("test"));
        assertEquals(true, rs.wasNull());
    }


    @Test
    public void connectFailover() throws SQLException {
        Assume.assumeTrue(hostname != null);
        String hosts = hostname + ":" + port + "," + hostname + ":" + (port + 1);
        String url = "jdbc:mariadb://" + hosts + "/" + database + "?user=" + username;
        url += (password != null && !"".equals(password) ? "&password=" + password : "");

        Connection connection = null;
        try {
            connection = openNewConnection(url);
            MariaDbConnection my = (MariaDbConnection) connection;
            assertTrue(my.getPort() == port);
            ResultSet rs = connection.createStatement().executeQuery("select 1");
            if (rs.next()) {
                assertEquals(rs.getInt(1), 1);
            } else {
                fail();
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void floatingNumbersTest() throws SQLException {

        PreparedStatement ps = sharedConnection.prepareStatement("insert into test_float (a) values (?)");
        ps.setDouble(1, 3.99);
        ps.executeUpdate();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select a from test_float");
        assertEquals(true, rs.next());
        assertEquals((float) 3.99, rs.getFloat(1), 0.00001);
        assertEquals((float) 3.99, rs.getFloat("a"), 0.00001);
        assertEquals(false, rs.next());
    }


    @Test
    public void manyColumnsTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("drop table if exists test_many_columns");
        StringBuilder query = new StringBuilder("create table test_many_columns (a0 int primary key not null");
        for (int i = 1; i < 1000; i++) {
            query.append(",a").append(i).append(" int");
        }
        query.append(")");
        stmt.execute(query.toString());
        query = new StringBuilder("insert into test_many_columns values (0");
        for (int i = 1; i < 1000; i++) {
            query.append(",").append(i);
        }
        query.append(")");
        stmt.execute(query.toString());
        ResultSet rs = stmt.executeQuery("select * from test_many_columns");

        assertEquals(true, rs.next());

        for (int i = 0; i < 1000; i++) {
            assertEquals(rs.getInt("a" + i), i);
        }

    }

    @Test
    public void bigAutoIncTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("alter table test_big_autoinc2 auto_increment = 1000");
        stmt.execute("insert into test_big_autoinc2 values (null, 'hej')", Statement.RETURN_GENERATED_KEYS);
        ResultSet rsGen = stmt.getGeneratedKeys();
        assertEquals(true, rsGen.next());
        assertEquals(1000, rsGen.getInt(1));
        stmt.execute("alter table test_big_autoinc2 auto_increment = " + Short.MAX_VALUE);
        stmt.execute("insert into test_big_autoinc2 values (null, 'hej')", Statement.RETURN_GENERATED_KEYS);
        rsGen = stmt.getGeneratedKeys();
        assertEquals(true, rsGen.next());
        assertEquals(Short.MAX_VALUE, rsGen.getInt(1));
        stmt.execute("alter table test_big_autoinc2 auto_increment = " + Integer.MAX_VALUE);
        stmt.execute("insert into test_big_autoinc2 values (null, 'hej')", Statement.RETURN_GENERATED_KEYS);
        rsGen = stmt.getGeneratedKeys();
        assertEquals(true, rsGen.next());
        assertEquals(Integer.MAX_VALUE, rsGen.getInt(1));
    }

    @Test
    public void bigUpdateCountTest() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        for (int i = 0; i < 4; i++) {
            stmt.execute("insert into test_big_update values (" + i + "," + i + ")");
        }
        ResultSet rs = stmt.executeQuery("select count(*) from test_big_update");
        assertEquals(true, rs.next());
        assertEquals(4, rs.getInt(1));
        int updateCount = stmt.executeUpdate("update test_big_update set updateme=updateme+1");
        assertEquals(4, updateCount);
    }


    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testException1() throws SQLException {
        sharedConnection.createStatement().execute("insert into extest values (1)");
        sharedConnection.createStatement().execute("insert into extest values (1)");
    }

    @Test
    public void testExceptionDivByZero() throws SQLException {
        ResultSet rs = sharedConnection.createStatement().executeQuery("select 1/0");
        assertEquals(rs.next(), true);
        assertEquals(null, rs.getString(1));
    }

    @Test(expected = SQLSyntaxErrorException.class)
    public void testSyntaxError() throws SQLException {
        sharedConnection.createStatement().executeQuery("create asdf b");
    }


    @Test
    public void testPreparedStatementsWithComments() throws SQLException {
        String query = "INSERT INTO commentPreparedStatements (a) VALUES (?) # ?";
        PreparedStatement pstmt = sharedConnection.prepareStatement(query);
        pstmt.setString(1, "yeah");
        pstmt.execute();
    }

    @Test
    public void testPreparedStatementsWithQuotes() throws SQLException {
        String query = "INSERT INTO quotesPreparedStatements (a,b) VALUES ('hellooo?', ?) # ?";
        PreparedStatement pstmt = sharedConnection.prepareStatement(query);
        pstmt.setString(1, "ff");
        pstmt.execute();
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    public void testResultSetPositions() throws SQLException {
        sharedConnection.createStatement().execute("insert into ressetpos values (1),(2),(3),(4)");
        Statement stmt = sharedConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery("select * from ressetpos");
        assertTrue(rs.isBeforeFirst());
        assertTrue(rs.next());
        assertFalse(rs.isBeforeFirst());
        assertTrue(rs.isFirst());
        rs.beforeFirst();
        assertTrue(rs.isBeforeFirst());
        while (rs.next()) {
            //just load datas.
        }
        assertTrue(rs.isAfterLast());
        rs.absolute(4);
        assertFalse(rs.isAfterLast());
        rs.absolute(2);
        assertEquals(2, rs.getInt(1));
        rs.relative(2);
        assertEquals(4, rs.getInt(1));
        assertFalse(rs.next());
        rs.previous();
        assertEquals(4, rs.getInt(1));
        rs.relative(-3);
        assertEquals(1, rs.getInt(1));
        assertEquals(false, rs.relative(-1));
        assertEquals(1, rs.getInt(1));
        rs.last();
        assertEquals(4, rs.getInt(1));
        assertEquals(4, rs.getRow());
        assertTrue(rs.isLast());
        rs.first();
        assertEquals(1, rs.getInt(1));
        assertEquals(1, rs.getRow());
        rs.absolute(-1);
        assertEquals(4, rs.getRow());
        assertEquals(4, rs.getInt(1));
    }

    @Test(expected = SQLException.class)
    public void findColumnTest() throws SQLException {
        ResultSet rs = sharedConnection.createStatement().executeQuery("select 1 as 'hej'");
        assertEquals(1, rs.findColumn("hej"));

        rs.findColumn("nope");

    }

    @Test
    public void getStatementTest() throws SQLException {
        Statement stmt1 = sharedConnection.createStatement();
        ResultSet rs = stmt1.executeQuery("select 1 as 'hej'");
        assertEquals(stmt1, rs.getStatement());
    }

    @Test
    public void testAutocommit() throws SQLException {
        assertTrue(sharedConnection.getAutoCommit());
        sharedConnection.setAutoCommit(false);
        assertFalse(sharedConnection.getAutoCommit());
        
        /* Check that autocommit value "false" , that driver derives from server status flags
         * remains the same when EOF, ERROR or OK stream were received.
         */
        sharedConnection.createStatement().executeQuery("select 1");
        assertFalse(sharedConnection.getAutoCommit());
        sharedConnection.createStatement().execute("set @a=1");
        assertFalse(sharedConnection.getAutoCommit());
        try {
            sharedConnection.createStatement().execute("insert into nosuchtable values(1)");
        } catch (Exception e) {
            //eat exception
        }
        assertFalse(sharedConnection.getAutoCommit());
        ResultSet rs = sharedConnection.createStatement().executeQuery("select @@autocommit");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));


        sharedConnection.setAutoCommit(true);
        
        /* Check that autocommit value "true" , that driver derives from server status flags
         * remains the same when EOF, ERROR or OK stream were received.
         */
        assertTrue(sharedConnection.getAutoCommit());
        sharedConnection.createStatement().execute("set @a=1");
        assertTrue(sharedConnection.getAutoCommit());
        try {
            sharedConnection.createStatement().execute("insert into nosuchtable values(1)");
        } catch (Exception e) {
            //eat exception
        }
        assertTrue(sharedConnection.getAutoCommit());
        rs = sharedConnection.createStatement().executeQuery("select @@autocommit");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        
        /* Set autocommit value using Statement.execute */
        sharedConnection.createStatement().execute("set @@autocommit=0");
        assertFalse(sharedConnection.getAutoCommit());

        sharedConnection.createStatement().execute("set @@autocommit=1");
        assertTrue(sharedConnection.getAutoCommit());
        
        /* Use session variable to set autocommit to 0 */
        Connection connection = null;
        try {
            connection = setConnection("&sessionVariables=autocommit=0");
            assertFalse(connection.getAutoCommit());
            sharedConnection.setAutoCommit(true);
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void testUpdateCountSingle() throws SQLException {
        Statement stmt = sharedConnection.createStatement();
        stmt.execute("select 1");
        assertTrue(-1 == stmt.getUpdateCount());
    }

    @Test
    public void testUpdateCountMulti() throws SQLException {
        Connection connection = null;
        try {
            connection = setConnection("&allowMultiQueries=true");
            Statement stmt = connection.createStatement();
            stmt.execute("select 1;select 1");
            assertTrue(-1 == stmt.getUpdateCount());
            stmt.getMoreResults();
            assertTrue(-1 == stmt.getUpdateCount());
        } finally {
            if (connection != null) connection.close();
        }
    }

    /**
     * CONJ-385 - stored procedure update count regression.
     *
     * @throws SQLException if connection error occur.
     */
    @Test
    public void testUpdateCountProcedure() throws SQLException {
        createProcedure("multiUpdateCount", "() BEGIN  SELECT 1; SELECT 2; END");
        CallableStatement callableStatement = sharedConnection.prepareCall("{call multiUpdateCount()}");
        callableStatement.execute();
        assertTrue(-1 == callableStatement.getUpdateCount());
        callableStatement.getMoreResults();
        assertTrue(-1 == callableStatement.getUpdateCount());
    }


    @Test
    public void testConnectWithDb() throws SQLException {
        Assume.assumeTrue(System.getenv("MAXSCALE_VERSION") == null);

        requireMinimumVersion(5, 0);
        try {
            sharedConnection.createStatement().executeUpdate("drop database test_testdrop");
        } catch (Exception e) {
            //eat exception
        }
        Connection connection = null;
        try {
            connection = setConnection("&createDatabaseIfNotExist=true&profileSql=true", "test_testdrop");
            DatabaseMetaData dbmd = connection.getMetaData();
            ResultSet rs = dbmd.getCatalogs();
            boolean foundDb = false;
            while (rs.next()) {
                if (rs.getString("table_cat").equals("test_testdrop")) {
                    foundDb = true;
                }
            }
            assertTrue(foundDb);
            sharedConnection.createStatement().executeUpdate("drop database test_testdrop");
        } finally {
            if (connection != null) connection.close();
        }
    }


    @Test
    public void streamingResult() throws SQLException {
        Statement st = sharedConnection.createStatement();

        for (int i = 0; i < 100; i++) {
            st.execute("insert into streamingtest values('aaaaaaaaaaaaaaaaaa')");
        }
        st.setFetchSize(Integer.MIN_VALUE);
        ResultSet rs = null;
        try {
            rs = st.executeQuery("select * from streamingtest");
            rs.next();
            Statement st2 = sharedConnection.createStatement();
            ResultSet rs2 = null;
            try {
                rs2 = st2.executeQuery("select * from streamingtest");
                rs2.next();
            } finally {
                rs2.close();
            }
        } finally {
            rs.close();
        }
    }

    // Test if driver works with sql_mode= NO_BACKSLASH_ESCAPES
    @Test
    public void noBackslashEscapes() throws SQLException {
        requireMinimumVersion(5, 0);

        // super privilege is needed for this test
        Assume.assumeTrue(hasSuperPrivilege("NoBackslashEscapes"));

        Statement st = sharedConnection.createStatement();
        ResultSet rs = st.executeQuery("select @@global.sql_mode");
        rs.next();
        String originalSqlMode = rs.getString(1);
        st.execute("set @@global.sql_mode = '" + originalSqlMode + ",NO_BACKSLASH_ESCAPES'");

        try {
            Connection connection = null;
            try {
                connection = setConnection("&profileSql=true");
                PreparedStatement preparedStatement =
                        connection.prepareStatement("insert into testBlob2(a) values(?)");
                byte[] bytes = new byte[255];
                for (byte i = -128; i < 127; i++) {
                    bytes[i + 128] = i;
                }
                MariaDbBlob blob = new MariaDbBlob(bytes);
                preparedStatement.setBlob(1, blob);
                int affectedRows = preparedStatement.executeUpdate();
                assertEquals(affectedRows, 1);
            } finally {
                if (connection != null) connection.close();
            }
        } finally {
            st.execute("set @@global.sql_mode='" + originalSqlMode + "'");
        }
    }

    // Test if driver works with sql_mode= NO_BACKSLASH_ESCAPES
    @Test
    public void noBackslashEscapes2() throws SQLException {
        requireMinimumVersion(5, 0);

        // super privilege is needed for this test
        Assume.assumeTrue(hasSuperPrivilege("NoBackslashEscapes2"));

        Statement st = sharedConnection.createStatement();
        ResultSet rs = st.executeQuery("select @@global.sql_mode");
        rs.next();
        String originalSqlMode = rs.getString(1);
        st.execute("set @@global.sql_mode = '" + originalSqlMode + ",NO_BACKSLASH_ESCAPES'");

        try {
            Connection connection = null;
            try {
                connection = setConnection("&profileSql=true");
                PreparedStatement preparedStatement =
                             connection.prepareStatement("insert into testString2(a) values(?)");
                preparedStatement.setString(1, "'\\");
                int affectedRows = preparedStatement.executeUpdate();
                assertEquals(affectedRows, 1);

                preparedStatement = connection.prepareStatement("select * from testString2");
                rs = preparedStatement.executeQuery();
                rs.next();
                String out = rs.getString(1);
                assertEquals(out, "'\\");

                Statement st2 = connection.createStatement();
                rs = st2.executeQuery("select 'a\\b\\c'");
                rs.next();
                assertEquals("a\\b\\c", rs.getString(1));
            } finally {
                if (connection != null) connection.close();
            }
        } finally {
            st.execute("set @@global.sql_mode='" + originalSqlMode + "'");
        }
    }

    // Test if driver works with sql_mode= ANSI_QUOTES
    @Test
    public void ansiQuotes() throws SQLException {

        // super privilege is needed for this test
        Assume.assumeTrue(hasSuperPrivilege("AnsiQuotes"));

        Statement st = sharedConnection.createStatement();
        ResultSet rs = st.executeQuery("select @@global.sql_mode");
        rs.next();
        String originalSqlMode = rs.getString(1);
        st.execute("set @@global.sql_mode = '" + originalSqlMode + ",ANSI_QUOTES'");

        try {
            Connection connection = null;
            try {
                connection = setConnection("&profileSql=true");
                PreparedStatement preparedStatement =
                        connection.prepareStatement("insert into testBlob2(a) values(?)");
                byte[] bytes = new byte[255];
                for (byte i = -128; i < 127; i++) {
                    bytes[i + 128] = i;
                }
                MariaDbBlob blob = new MariaDbBlob(bytes);
                preparedStatement.setBlob(1, blob);
                int affectedRows = preparedStatement.executeUpdate();
                assertEquals(affectedRows, 1);
            } finally {
                if (connection != null) connection.close();
            }
        } finally {
            st.execute("set @@global.sql_mode='" + originalSqlMode + "'");
        }
    }

    @Test
    public void unsignedTest() throws Exception {
        Statement st = sharedConnection.createStatement();
        st.execute("insert into unsignedtest values(4294967295)");
        ResultSet rs = st.executeQuery("select * from unsignedtest");
        assertTrue(rs.next());
        assertEquals(4294967295L, rs.getLong("unsignedtest.a"));
    }


    @Test
    // Bug in URL parser
    public void mdev3916() throws Exception {
        try {
            setConnection("&password=");
        } catch (SQLException ex) {
            //SQLException is ok because we might get for example an access denied exception
            if (!(ex.getMessage().contains("Could not connect: Access denied"))) {
                throw ex;
            }
        }
    }

    @Test
    public void conj1() throws Exception {
        Assume.assumeTrue(System.getenv("MAXSCALE_VERSION") == null);

        requireMinimumVersion(5, 0);
        Connection connection = null;
        try {
            connection = setConnection("&profileSql=true");
            Statement st = connection.createStatement();
            st.setQueryTimeout(1);
            st.execute("select sleep(0.5)");
            try {
                st.execute("select * from information_schema.columns as c1,  information_schema.tables, information_schema.tables as t2");
                fail("must be exception here");
            } catch (Exception e) {
                //normal exception
            }

            Statement st2 = connection.createStatement();
            assertEquals(st2.getQueryTimeout(), 0);
            // no exception
            ResultSet rs = st2.executeQuery("select sleep(1.5)");
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
            Statement st3 = connection.createStatement();

            st3.setQueryTimeout(1);
            rs = st3.executeQuery("select sleep(0.1)");
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
            assertEquals(st3.getQueryTimeout(), 1);
        } finally {
            if (connection != null) connection.close();
        }
    }

    /* Check that exception contains SQL statement, for queries with syntax errors */
    @Test
    public void dumpQueryOnSyntaxException() throws Exception {
        String syntacticallyWrongQuery = "banana";
        try {
            Statement st = sharedConnection.createStatement();
            st.execute(syntacticallyWrongQuery);
        } catch (SQLException sqle) {
            assertTrue(sqle.getCause().getMessage().contains("Query is: " + syntacticallyWrongQuery));
        }
    }

    /* Check that query contains SQL statement, if dumpQueryOnException is true */
    @Test
    public void dumpQueryOnException() throws Exception {
        Connection connection = null;
        try {
            connection = setConnection("&profileSql=true&dumpQueriesOnException=true");
            String selectFromNonExistingTable = "select * from banana";
            try {
                Statement st = connection.createStatement();
                st.execute(selectFromNonExistingTable);
            } catch (SQLException sqle) {
                assertTrue(sqle.getCause().getMessage().contains("Query is: " + selectFromNonExistingTable));
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    /* CONJ-14 
     * getUpdateCount(), getResultSet() should indicate "no more results" with
     * (getUpdateCount() == -1 && getResultSet() == null)  
     */
    @Test
    public void conj14() throws Exception {
        Statement st = sharedConnection.createStatement();
        
        /* 1. Test update statement */
        st.execute("use " + database);
        assertEquals(0, st.getUpdateCount());

        /* No more results */
        assertFalse(st.getMoreResults());
        assertEquals(-1, st.getUpdateCount());
        assertEquals(null, st.getResultSet());
        
        /* 2. Test select statement */
        st.execute("select 1");
        assertEquals(-1, st.getUpdateCount());
        assertTrue(st.getResultSet() != null);
        
        /* No More results */
        assertFalse(st.getMoreResults());
        assertEquals(-1, st.getUpdateCount());
        assertEquals(null, st.getResultSet());
        
        /* Test batch  */
        Connection connection = null;
        try {
            connection = setConnection("&profileSql=true&allowMultiQueries=true");
            st = connection.createStatement();
            
            /* 3. Batch with two SELECTs */

            st.execute("select 1;select 2");
            /* First result (select)*/
            assertEquals(-1, st.getUpdateCount());
            assertTrue(st.getResultSet() != null);
            
            /* has more results */
            assertTrue(st.getMoreResults()); 
            
            /* Second result (select) */
            assertEquals(-1, st.getUpdateCount());
            assertTrue(st.getResultSet() != null);
            
            /* no more results */
            assertFalse(st.getMoreResults());
            assertEquals(-1, st.getUpdateCount());
            assertEquals(null, st.getResultSet());
            
            /* 4. Batch with a SELECT and non-SELECT */

            st.execute("select 1; use " + database);
            /* First result (select)*/
            assertEquals(-1, st.getUpdateCount());
            assertTrue(st.getResultSet() != null);
            
            /* Next result is no ResultSet */
            assertTrue(st.getMoreResults());
            assertNull(st.getResultSet());
            assertEquals(0, st.getUpdateCount());

            /* no more results */
            assertFalse(st.getMoreResults());
            assertEquals(-1, st.getUpdateCount());
            assertEquals(null, st.getResultSet());
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void conj25() throws Exception {
        Statement stmt = null;
        try {
            stmt = sharedConnection.createStatement();
            StringBuilder st = new StringBuilder("INSERT INTO conj25 VALUES (REPEAT('a',1024))");
            for (int i = 1; i <= 100; i++) {
                st.append(",(REPEAT('a',1024))");
            }
            stmt.setFetchSize(Integer.MIN_VALUE);
            stmt.execute(st.toString());
            stmt.executeQuery("SELECT * FROM conj25 a, conj25 b");
        } finally {
            stmt.close();
        }

    }

    @Test
    public void namedPipe() throws Exception {
        ResultSet rs = null;
        try {
            rs = sharedConnection.createStatement().executeQuery("select @@named_pipe,@@socket");
            rs.next();
            if (rs.getBoolean(1)) {
                String namedPipeName = rs.getString(2);
                //skip test if no namedPipeName was obtained because then we do not use a socket connection
                Assume.assumeTrue(namedPipeName != null);
                Connection connection = null;
                try {
                    connection = setConnection("&pipe=" + namedPipeName);
                    Statement stmt = connection.createStatement();
                    ResultSet rs2 = stmt.executeQuery("SELECT 1");
                    assertTrue(rs2.next());
                } finally {
                    if (connection != null) connection.close();
                }
            }
        } catch (SQLException e) {
            //not on windows
        }
    }

    /**
     * CONJ-435 : "All pipe instances are busy" exception on multiple connections to the same named pipe.
     *
     * @throws Exception if any error occur.
     */
    @Test
    public void namedPipeBusyTest() throws Exception {
        try {
            ResultSet rs = sharedConnection.createStatement().executeQuery("select @@named_pipe,@@socket");
            assertTrue(rs.next());
            if (rs.getBoolean(1)) {
                String namedPipeName = rs.getString(2);
                //skip test if no namedPipeName was obtained because then we do not use a socket connection
                Assume.assumeTrue(namedPipeName != null);
                ExecutorService exec = Executors.newFixedThreadPool(100);
                //check blacklist shared
                for (int i = 0; i < 100; i++) {
                    exec.execute(new ConnectWithPipeThread("jdbc:mariadb:///testj?user="
                            + username + "&pipe=" + namedPipeName + "&connectTimeout=500"));
                }

                //wait for thread endings
                exec.shutdown();

                exec.awaitTermination(30, TimeUnit.SECONDS);
            }
        } catch (SQLException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Unknown system variable 'named_pipe'"));
        }
    }

    /**
     * CONJ-293 : permit connection to named pipe when no host is defined.
     */
    @Test
    public void namedPipeWithoutHost() {
        ResultSet rs = null;
        try {
            rs = sharedConnection.createStatement().executeQuery("select @@named_pipe,@@socket");
            assertTrue(rs.next());
            if (rs.getBoolean(1)) {
                String namedPipeName = rs.getString(2);
                //skip test if no namedPipeName was obtained because then we do not use a socket connection
                Assume.assumeTrue(namedPipeName != null);
                Connection connection = null;
                try {
                    connection = DriverManager.getConnection("jdbc:mariadb:///testj?user="
                            + username + "&pipe=" + namedPipeName);
                    Statement stmt = connection.createStatement();
                    ResultSet rs2 = stmt.executeQuery("SELECT 1");
                    assertTrue(rs2.next());
                } finally {
                    if (connection != null) connection.close();
                }
            }
        } catch (SQLException e) {
            //not on windows
        }
    }

    @Test
    public void localSocket() throws Exception {
        requireMinimumVersion(5, 1);
        Assume.assumeTrue(System.getenv("TRAVIS") == null);
        Assume.assumeTrue(isLocalConnection("localSocket"));

        Statement st = sharedConnection.createStatement();
        ResultSet rs = st.executeQuery("select @@version_compile_os,@@socket");
        if (!rs.next()) {
            return;
        }
        System.out.println("os:" + rs.getString(1) + " path:" + rs.getString(2));
        String os = rs.getString(1);
        if (os.toLowerCase().startsWith("win")) {
            return;
        }

        String path = rs.getString(2);
        Connection connection = null;
        try {
            connection = setConnection("&localSocket=" + path + "&profileSql=true");
            rs = connection.createStatement().executeQuery("select 1");
            assertTrue(rs.next());
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void sharedMemory() throws Exception {
        requireMinimumVersion(5, 1);
        Statement st = sharedConnection.createStatement();
        ResultSet rs = st.executeQuery("select @@version_compile_os");
        if (!rs.next()) {
            return;
        }

        String os = rs.getString(1);
        if (!os.toLowerCase().startsWith("win")) {
            return;  // skip test on non-Windows
        }

        rs = st.executeQuery("select @@shared_memory,@@shared_memory_base_name");
        if (!rs.next()) {
            return;
        }

        if (!rs.getString(1).equals("1")) {
            return;
        }

        String shmBaseName = rs.getString(2);
        Connection connection = null;
        try {
            connection = setConnection("&sharedMemory=" + shmBaseName + "&profileSql=true");
            rs = connection.createStatement().executeQuery("select repeat('a',100000)");
            assertTrue(rs.next());
            assertEquals(100000, rs.getString(1).length());
            char[] arr = new char[100000];
            Arrays.fill(arr, 'a');
            rs = connection.createStatement().executeQuery("select '" + new String(arr) + "'");
            assertTrue(rs.next());
            assertEquals(100000, rs.getString(1).length());
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void preparedStatementToString() throws Exception {
        PreparedStatement ps = null;
        try {
            ps = sharedConnection.prepareStatement("SELECT ?,?,?,?,?,?");
            ps.setInt(1, 1);
            ps.setBigDecimal(2, new BigDecimal("1"));
            ps.setString(3, "one");
            ps.setBoolean(4, true);
            Calendar calendar = new GregorianCalendar(1972, 3, 22);
            ps.setDate(5, new Date(calendar.getTime().getTime()));
            ps.setDouble(6, 1.5);
            assertEquals("sql : 'SELECT ?,?,?,?,?,?', parameters : [1,1,'one',1,'1972-04-22',1.5]", ps.toString());
        } finally {
            ps.close();
        }
    }

    /* Test that CLOSE_CURSORS_ON_COMMIT is silently ignored, and HOLD_CURSORS_OVER_COMMIT is actually used*/
    @Test
    public void resultSetHoldability() throws Exception {
        Statement st = sharedConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, st.getResultSetHoldability());
        PreparedStatement ps = sharedConnection.prepareStatement("SELECT 1", ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, ps.getResultSetHoldability());
        ResultSet rs = ps.executeQuery();
        assertEquals(rs.getHoldability(), ResultSet.HOLD_CURSORS_OVER_COMMIT);
        CallableStatement cs = sharedConnection.prepareCall("{CALL foo}", ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        assertEquals(cs.getResultSetHoldability(), ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Test
    public void emptyBatch() throws Exception {
        Statement st = sharedConnection.createStatement();
        st.executeBatch();
    }

    @Test
    public void createDbWithSpacesTest() throws SQLException {
        Connection connection = null;
        try {
            connection = setConnection("&createDatabaseIfNotExist=true&profileSql=true", "test with spaces");
            DatabaseMetaData dbmd = connection.getMetaData();
            ResultSet rs = dbmd.getCatalogs();
            boolean foundDb = false;
            while (rs.next()) {
                if (rs.getString("table_cat").equals("test with spaces")) {
                    foundDb = true;
                }
            }
            assertTrue("database \"test with spaces\" not created !?", foundDb);
            connection.createStatement().execute("drop database `test with spaces`");
        } finally {
            if (connection != null) connection.close();
        }
    }

    /**
     * CONJ-275 : Streaming resultSet with no result must not have a next() value to true.
     *
     * @throws Exception exception
     */
    @Test
    public void checkStreamingWithoutResult() throws Exception {
        PreparedStatement pstmt = sharedConnection.prepareStatement("SELECT * FROM conj275 where a = ?");
        pstmt.setFetchSize(10);
        pstmt.setString(1, "no result");
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            fail("must not have result value");
        }
    }

    private static class ConnectWithPipeThread implements Runnable {
        private final String url;

        public ConnectWithPipeThread(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                Connection connection = null;
                try {
                    connection = DriverManager.getConnection(url);
                    Thread.sleep(1000);
                } finally {
                    if (connection != null) connection.close();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * CONJ-497 - Long escapable string.
     *
     * @throws SQLException exception
     */
    @Test
    public void testLongEscapes() throws SQLException {
        //40m, because escaping will double the send byte numbers
        Assume.assumeTrue(checkMaxAllowedPacketMore40m("testLongEscapes"));
        createTable("testLongEscapes", "t1 longtext");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = sharedConnection.prepareStatement("INSERT into testLongEscapes values (?)");
            byte[] arr = new byte[20000000];
            Arrays.fill(arr, (byte) '\'');
            preparedStatement.setBytes(1, arr);
            preparedStatement.execute();

            Arrays.fill(arr, (byte) '\"');
            preparedStatement.setBytes(1, arr);
            preparedStatement.execute();
        } finally {
            if (preparedStatement != null) preparedStatement.close();
        }

        Statement stmt = sharedConnection.createStatement();
        ResultSet rs = stmt.executeQuery("select length(t1) from testLongEscapes");
        assertTrue(rs.next());
        assertEquals(20000000, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(20000000, rs.getInt(1));
        assertFalse(rs.next());
    }

}
