package com.sack.rpgroll.database;

import java.sql.Connection;

public interface DatabaseProvider {

    void connect();

    void disconnect();

    boolean isConnected();

    Connection getConnection();

}