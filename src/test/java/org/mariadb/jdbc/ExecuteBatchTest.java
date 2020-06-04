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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ExecuteBatchTest extends BaseTest {

    static String oneHundredLengthString = "";
    static boolean profileSql = true;

    static {
        char[] chars = new char[100];
        for (int i = 27; i < 127; i++) {
            chars[i - 27] = (char) i;
        }
        oneHundredLengthString = new String(chars);
    }

    /**
     * Create test tables.
     *
     * @throws SQLException if connection error occur
     */
    @BeforeClass()
    public static void initClass() throws SQLException {
        createTable("ExecuteBatchTest", "id int not null primary key auto_increment, test varchar(100) , test2 int");
        createTable("ExecuteBatchUseBatchMultiSend", "test varchar(100)");
    }

    /**
     * CONJ-426: Test that executeBatch can be properly interrupted.
     *
     * @throws Exception If the test fails
     */
    @Test
    public void interruptExecuteBatch() throws Exception {
        Assume.assumeTrue(sharedOptions().useBatchMultiSend);
        ExecutorService service = Executors.newFixedThreadPool(1);

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        final AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>();

        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement preparedStatement = sharedConnection.prepareStatement(
                            "INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");

                    // Send a large enough batch that will take long enough to allow us to interrupt it
                    for (int i = 0; i < 1000000; i++) {
                        preparedStatement.setString(1, String.valueOf(System.nanoTime()));
                        preparedStatement.setInt(2, i);
                        preparedStatement.addBatch();
                    }

                    barrier.await();

                    preparedStatement.executeBatch();
                } catch (InterruptedException ex) {
                    exceptionRef.set(ex);
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException ex) {
                    exceptionRef.set(ex);
                } catch (SQLException ex) {
                    exceptionRef.set(ex);
                    wasInterrupted.set(Thread.currentThread().isInterrupted());
                } catch (Exception ex) {
                    exceptionRef.set(ex);
                }
            }
        });

        barrier.await();

        // Allow the query time to send
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));

        // Interrupt the thread
        service.shutdownNow();

        assertTrue(
                service.awaitTermination(1, TimeUnit.MINUTES)
        );

        assertNotNull(exceptionRef.get());

        //ensure that even interrupted, connection status is when sending in bulk (all corresponding bulk send are read)
        ResultSet rs = sharedConnection.createStatement().executeQuery("SELECT 123456");
        assertTrue(rs.next());
        assertEquals(123456, rs.getInt(1));

        StringWriter writer = new StringWriter();
        exceptionRef.get().printStackTrace(new PrintWriter(writer));

        assertTrue(
                "Exception should be a SQLException: \n" + writer.toString(),
                exceptionRef.get() instanceof SQLException
        );

        assertTrue(wasInterrupted.get());

    }

    @Test
    public void serverBulk8mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("serverBulk8mTest"));
        Assume.assumeTrue(runLongTest);
        Assume.assumeFalse(sharedIsAurora());

        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        Connection connection = null;
        try {
            connection = setConnection("&useComMulti=false&useBatchMultiSend=true&profileSql=" + profileSql);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            //packet size : 7 200 068 kb
            addBatchData(preparedStatement, 60000, connection);
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void serverBulk20mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore20m("serverBulk20mTest"));
        Assume.assumeTrue(runLongTest);
        Assume.assumeFalse(sharedIsAurora());

        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        Connection connection = null;
        try {
            connection = setConnection("&useComMulti=false&useBatchMultiSend=true&profileSql=" + profileSql);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            //packet size : 7 200 068 kb
            addBatchData(preparedStatement, 160000, connection);
        } finally {
            if (connection != null) connection.close();
        }
    }


    @Test
    public void serverStd8mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("serverStd8mTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        Connection connection = null;
        try {
            connection = setConnection("&useComMulti=false&useBatchMultiSend=false&profileSql=" + profileSql);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            addBatchData(preparedStatement, 60000, connection);
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void clientBulkTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("serverStd8mTest"));
        Assume.assumeTrue(runLongTest);
        Assume.assumeFalse(sharedIsAurora());

        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        Connection connection = null;
        try {
            connection = setConnection("&useComMulti=false&useBatchMultiSend=true&useServerPrepStmts=false&profileSql=" + profileSql);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            addBatchData(preparedStatement, 60000, connection);
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void clientRewriteValuesNotPossible8mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("clientRewriteValuesNotPossibleTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        Connection connection = null;
        try {
            connection = setConnection("&rewriteBatchedStatements=true&profileSql=" + profileSql);
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO ExecuteBatchTest(test, test2) values (?, ?) ON DUPLICATE KEY UPDATE id=?");
            addBatchData(preparedStatement, 60000, connection, true);
        } finally {
            if (connection != null) connection.close();
        }
    }


    @Test
    public void clientRewriteValuesNotPossible20mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("clientRewriteValuesNotPossibleTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        Connection connection = null;
        try {
            connection = setConnection("&rewriteBatchedStatements=true&profileSql=" + profileSql);
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO ExecuteBatchTest(test, test2) values (?, ?) ON DUPLICATE KEY UPDATE id=?");
            addBatchData(preparedStatement, 160000, connection, true);
        } finally {
            if (connection != null) connection.close();
        }
    }

    @Test
    public void clientRewriteValuesPossibleTest() throws SQLException {
        // 8mb
        // 20mb
        // 40mb
    }

    @Test
    public void clientRewriteMultiTest() throws SQLException {
        // 8mb
        // 20mb
        // 40mb
    }

    @Test
    public void clientStdMultiTest() throws SQLException {
        // 8mb
        // 20mb
        // 40mb
    }

    private void addBatchData(PreparedStatement preparedStatement, int batchNumber, Connection connection) throws SQLException {
        addBatchData(preparedStatement, batchNumber, connection, false);
    }

    private void addBatchData(PreparedStatement preparedStatement, int batchNumber, Connection connection, boolean additionnalParameter)
            throws SQLException {
        for (int i = 0; i < batchNumber; i++) {
            preparedStatement.setString(1, oneHundredLengthString);
            preparedStatement.setInt(2, i);
            if (additionnalParameter) preparedStatement.setInt(3, i);
            preparedStatement.addBatch();
        }
        int[] resultInsert = preparedStatement.executeBatch();

        //test result Size
        assertEquals(batchNumber, resultInsert.length);
        for (int i = 0; i < batchNumber; i++) {
            assertEquals(1, resultInsert[i]);
        }

        //check that connection is OK and results are well inserted
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM ExecuteBatchTest");
        for (int i = 0; i < batchNumber; i++) {
            assertTrue(resultSet.next());
            assertEquals(i + 1, resultSet.getInt(1));
            assertEquals(oneHundredLengthString, resultSet.getString(2));
            assertEquals(i, resultSet.getInt(3));
        }
        assertFalse(resultSet.next());
    }

    @Test
    public void useBatchMultiSend() throws Exception {
        Assume.assumeFalse(sharedIsAurora());
        Connection connection = null;
        try {
            connection = setConnection("&useBatchMultiSend=true");
            String sql = "insert into ExecuteBatchUseBatchMultiSend (test) values (?)";
            PreparedStatement pstmt = null;
            try {
                pstmt = connection.prepareStatement(sql);
                for (int i = 0; i < 10; i++) {
                    pstmt.setInt(1, i);
                    pstmt.addBatch();
                }
                int[] updateCounts = pstmt.executeBatch();
                assertEquals(10, updateCounts.length);
                for (int i = 0; i < updateCounts.length; i++) {
                    assertEquals(sharedIsRewrite() ? Statement.SUCCESS_NO_INFO : 1, updateCounts[i]);
                }
            } finally {
                pstmt.close();
            }
        } finally {
            if (connection != null) connection.close();
        }
    }
}
