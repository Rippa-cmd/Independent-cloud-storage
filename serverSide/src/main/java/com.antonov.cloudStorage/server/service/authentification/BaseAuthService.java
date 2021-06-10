package com.antonov.cloudStorage.server.service.authentification;

import com.antonov.cloudStorage.server.interfaces.AuthService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс аутентификации пользователей, хранящий необходимую для этой операции информацию о пользователях
 */
public class BaseAuthService implements AuthService {

    private Statement statement = null;
    private ResultSet set = null;
    PreparedStatement preparedStatement = null;

    // Добавление нового пользователя
    @Override
    public boolean addEntry(String[] logPassNick) {
        try {
            preparedStatement = DBConnector.getConnection().prepareStatement("INSERT INTO credentials (login, password, nick) VALUES (?, ?, ?)");
            preparedStatement.setString(1, logPassNick[1]);
            preparedStatement.setString(2, logPassNick[2]);
            preparedStatement.setString(3, logPassNick[3]);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            //Server.logger.error(e);
            return false;
        }
    }

    @Override
    public void start() {
        try {
            statement = DBConnector.getConnection().createStatement();
        } catch (SQLException | ClassNotFoundException e) {
            //Server.logger.error(e);
        }
        //Server.logger.info("AuthService started");
    }

    @Override
    public void stop() {
        try {
            if (statement != null)
                statement.close();
        } catch (SQLException e) {
            //Server.logger.error(e);
        }
        //Server.logger.warn("AuthService stopped");
    }

    // Получение ника по логину и паролю
    @Override
    public List<String> getIdAndNicknameByLoginAndPassword(String login, String password) {
        try {

            // Сделал через prepared для игнорирования спец символов ('," и тд)
            preparedStatement = DBConnector.getConnection().prepareStatement("select id, nick from credentials where login = ? and password = ?");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            set = preparedStatement.executeQuery();
            List<String> idAndNick = new ArrayList<>();
            if (set.next()) {
                idAndNick.add(set.getString(1));
                idAndNick.add(set.getString(2));
                return idAndNick;
            }
        } catch (SQLException | ClassNotFoundException e) {
            //Server.logger.error(e);
        }
        return null;
    }

    // Заменяет ник в БД на новый
    public void nickChanger(String oldNick, String newNick) {
        try {
            preparedStatement = DBConnector.getConnection().prepareStatement("update users set nick = ? where nick = ?");
            preparedStatement.setString(1, newNick);
            preparedStatement.setString(2, oldNick);
            preparedStatement.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            //Server.logger.error(e);
        }
    }

    // Занят ли ник
//    @Override
//    public boolean isNicknameBusy(String nickname) {
//        try {
//            set = statement.executeQuery("select nick from users");
//            while (set.next()) {
//                if (set.getString(1).equals(nickname))
//                    return true;
//            }
//
//        } catch (SQLException e) {
//            //Server.logger.error(e);
//        }
//        return false;
//    }

    //Занят ли логин
    @Override
    public boolean isLoginBusy(String login) {
        try {
            set = statement.executeQuery("select login from users");
            while (set.next()) {
                if (set.getString(1).equals(login))
                    return true;
            }

        } catch (SQLException e) {
            //Server.logger.error(e);
        }
        return false;
    }
}
