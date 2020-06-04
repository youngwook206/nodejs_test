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

package org.mariadb.jdbc.failover;

import org.junit.*;
import org.mariadb.jdbc.internal.util.constant.HaMode;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Exemple mvn test  -DdefaultLoadbalanceUrl=jdbc:mariadb:loadbalance//localhost:3306,localhost:3307/test?user=root.
 */
public class LoadBalanceFailoverTest extends BaseMultiHostTest {
    /**
     * Initialisation.
     *
     * @throws SQLException exception
     */
    @BeforeClass()
    public static void beforeClass2() throws SQLException {
        proxyUrl = proxyLoadbalanceUrl;
        Assume.assumeTrue(initialLoadbalanceUrl != null);
    }

    /**
     * Initialisation.
     *
     * @throws SQLException exception
     */
    @Before
    public void init() throws SQLException {
        defaultUrl = initialLoadbalanceUrl;
        currentType = HaMode.LOADBALANCE;
    }

    @Test(expected = SQLException.class)
    public void failover() throws Throwable {
        Connection connection = null;
        try {
            connection = getNewConnection("&autoReconnect=true&retriesAllDown=6", true);
            int master1ServerId = getServerId(connection);
            stopProxy(master1ServerId);
            connection.createStatement().execute("SELECT 1");
        } finally {
            if (connection != null) connection.close();
        }
    }


    @Test
    public void randomConnection() throws Throwable {
        Assume.assumeTrue(initialLoadbalanceUrl.contains("loadbalance"));
        Map<String, MutableInt> connectionMap = new HashMap<String, MutableInt>();
        for (int i = 0; i < 20; i++) {
            Connection connection = null;
            try {
                connection = getNewConnection(false);
                int serverId = getServerId(connection);
                MutableInt count = connectionMap.get(String.valueOf(serverId));
                if (count == null) {
                    connectionMap.put(String.valueOf(serverId), new MutableInt());
                } else {
                    count.increment();
                }
            } finally {
                if (connection != null) connection.close();
            }
        }

        Assert.assertTrue(connectionMap.size() >= 2);
        for (String key : connectionMap.keySet()) {
            Integer connectionCount = connectionMap.get(key).get();
            Assert.assertTrue(connectionCount > 1);
        }
    }


    @Test
    public void testReadonly() throws SQLException {
        Connection connection = null;
        try {
            connection = getNewConnection(false);
            connection.setReadOnly(true);

            Statement stmt = connection.createStatement();
            stmt.execute("drop table  if exists multinode");
            stmt.execute("create table multinode (id int not null primary key auto_increment, test VARCHAR(10))");
        } catch (SQLException sqle) {
            //normal exception
        } finally {
            if (connection != null) connection.close();
        }
    }

    class MutableInt {
        int value = 1; // note that we start at 1 since we're counting

        public void increment() {
            ++value;
        }

        public int get() {
            return value;
        }
    }

}
