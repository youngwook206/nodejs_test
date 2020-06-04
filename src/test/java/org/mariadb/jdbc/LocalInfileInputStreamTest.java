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

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class LocalInfileInputStreamTest extends BaseTest {
    /**
     * Initialisation.
     *
     * @throws SQLException exception
     */
    @BeforeClass()
    public static void initClass() throws SQLException {
        createTable("LocalInfileInputStreamTest", "id int, test varchar(100)");
        createTable("ttlocal", "id int, test varchar(100)");
        createTable("ldinfile", "a varchar(10)");
        createTable("`infile`", "`a` varchar(50) DEFAULT NULL, `b` varchar(50) DEFAULT NULL",
                "ENGINE=InnoDB DEFAULT CHARSET=latin1");
    }

    @Test
    public void testLocalInfileInputStream() throws SQLException {
        Statement st = null;
        try {
            st = sharedConnection.createStatement();
            // Build a tab-separated record file
            StringBuilder builder = new StringBuilder();
            builder.append("1\thello\n");
            builder.append("2\tworld\n");

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes());
            ((MariaDbStatement) st).setLocalInfileInputStream(inputStream);

            st.executeUpdate("LOAD DATA LOCAL INFILE 'dummy.tsv' INTO TABLE LocalInfileInputStreamTest (id, test)");

            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM LocalInfileInputStreamTest");
            boolean next = rs.next();
            assertTrue(next);

            int count = rs.getInt(1);
            assertEquals(2, count);

            rs = st.executeQuery("SELECT * FROM LocalInfileInputStreamTest");

            validateRecord(rs, 1, "hello");
            validateRecord(rs, 2, "world");
        } finally {
            st.close();
        }
    }

    @Test
    public void testLocalInfileValidInterceptor() throws Exception {
        File temp = File.createTempFile("validateInfile", ".txt");
        StringBuilder builder = new StringBuilder();
        builder.append("1,hello\n");
        builder.append("2,world\n");
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(temp));
            bw.write(builder.toString());
        } finally {
            bw.close();
        }
        testLocalInfile(temp.getAbsolutePath().replace("\\", "/"));
    }

    @Test
    public void testLocalInfileUnValidInterceptor() throws Exception {
        File temp = File.createTempFile("localInfile", ".txt");
        StringBuilder builder = new StringBuilder();
        builder.append("1,hello\n");
        builder.append("2,world\n");
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(temp));
            bw.write(builder.toString());
        } finally {
            bw.close();
        }
        try {
            testLocalInfile(temp.getAbsolutePath().replace("\\", "/"));
            fail("Must have been intercepted");
        } catch (SQLException sqle) {
            assertTrue(sqle.getMessage().contains("LOCAL DATA LOCAL INFILE request to send local file named")
                    && sqle.getMessage().contains("not validated by interceptor \"org.mariadb.jdbc.LocalInfileInterceptorImpl\""));
        }
        //check that connection state is correct
        Statement st = sharedConnection.createStatement();
        ResultSet rs = st.executeQuery("SELECT 1");
        rs.next();
        assertEquals(1, rs.getInt(1));
    }


    private void testLocalInfile(String file) throws SQLException {
        Statement st = null;
        try {
            st = sharedConnection.createStatement();
            st.executeUpdate("LOAD DATA LOCAL INFILE '" + file
                    + "' INTO TABLE ttlocal "
                    + "  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'"
                    + "  (id, test)");

            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ttlocal");
            boolean next = rs.next();

            assertTrue(next);
            assertEquals(2, rs.getInt(1));

            rs = st.executeQuery("SELECT * FROM ttlocal");

            validateRecord(rs, 1, "hello");
            validateRecord(rs, 2, "world");
        } finally {
            st.close();
        }
    }

    @Test
    public void loadDataInfileEmpty() throws SQLException, IOException {
        // Create temp file.
        File temp = File.createTempFile("validateInfile", ".tmp");
        try {
            Statement st = sharedConnection.createStatement();
            st.execute("LOAD DATA LOCAL INFILE '" + temp.getAbsolutePath().replace('\\', '/')
                    + "' INTO TABLE ldinfile");
            ResultSet rs = st.executeQuery("SELECT * FROM ldinfile");
            assertFalse(rs.next());
        } finally {
            temp.delete();
        }
    }

    @Test
    public void testPrepareLocalInfileWithoutInputStream() throws SQLException {
        try {
            PreparedStatement st = sharedConnection.prepareStatement("LOAD DATA LOCAL INFILE 'validateInfile.tsv' "
                    + "INTO TABLE ldinfile");
            st.execute();
            fail();
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("Could not send file"));
            //check that connection is alright
            try {
                assertFalse(sharedConnection.isClosed());
                Statement st = sharedConnection.createStatement();
                st.execute("SELECT 1");
            } catch (SQLException eee) {
                fail();
            }
        }
    }

    private void validateRecord(ResultSet rs, int expectedId, String expectedTest) throws SQLException {
        boolean next = rs.next();
        assertTrue(next);

        int id = rs.getInt(1);
        String test = rs.getString(2);
        assertEquals(expectedId, id);
        assertEquals(expectedTest, test);
    }

    private File createTmpData(long recordNumber) throws Exception {
        File file = File.createTempFile("./infile" + recordNumber, ".tmp");

        //write it
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            // Every row is 8 bytes to make counting easier
            for (long i = 0; i < recordNumber; i++) {
                writer.write("\"a\",\"b\"");
                writer.write("\n");
            }
        } finally {
            writer.close();
        }

        return file;
    }

    private void checkBigLocalInfile(long fileSize) throws Exception {
        long recordNumber = fileSize / 8;
        Statement statement = null;
        try {
            statement = sharedConnection.createStatement();
            statement.execute("truncate `infile`");
            File file = createTmpData(recordNumber);
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(file));
                MariaDbStatement stmt = statement.unwrap(MariaDbStatement.class);
                stmt.setLocalInfileInputStream(is);
                int insertNumber = stmt.executeUpdate("LOAD DATA LOCAL INFILE 'ignoredFileName' "
                        + "INTO TABLE `infile` "
                        + "COLUMNS TERMINATED BY ',' ENCLOSED BY '\\\"' ESCAPED BY '\\\\' "
                        + "LINES TERMINATED BY '\\n' (`a`, `b`)");
                assertEquals(insertNumber, recordNumber);
            } finally {
                is.close();
            }

            statement.setFetchSize(1000); //to avoid using too much memory for tests
            ResultSet rs = null;
            try {
                rs = statement.executeQuery("SELECT * FROM `infile`");
                for (int i = 0; i < recordNumber; i++) {
                    assertTrue("record " + i + " doesn't exist", rs.next());
                    assertEquals("a", rs.getString(1));
                    assertEquals("b", rs.getString(2));
                }
                assertFalse(rs.next());
            } finally {
                rs.close();
            }

        } finally {
            statement.close();
        }
    }

    /**
     * CONJ-375 : error with local infile with size > 16mb.
     *
     * @throws Exception if error occus
     */
    @Test
    public void testSmallBigLocalInfileInputStream() throws Exception {
        checkBigLocalInfile(256);
    }

    @Test
    public void test2xBigLocalInfileInputStream() throws Exception {
        Assume.assumeTrue(checkMaxAllowedPacketMore40m("test2xBigLocalInfileInputStream"));
        checkBigLocalInfile(16777216 * 2);
    }

    @Test
    public void test2xMaxAllowedPacketLocalInfileInputStream() throws Exception {
        ResultSet rs = sharedConnection.createStatement().executeQuery("select @@max_allowed_packet");
        rs.next();
        long maxAllowedPacket = rs.getLong(1);
        checkBigLocalInfile(maxAllowedPacket * 2);
    }

}
