package auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountManager {
    private final DatabaseConnection databaseConnection;

    public AccountManager(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public int register(String username, String password) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String insertSQL = "INSERT INTO accounts(username, password) VALUES (?, ?)";

        try (Connection connection = databaseConnection.getSqlConnection();
             PreparedStatement statement = connection.prepareStatement(insertSQL, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean authenticate(String username, String password) {
        // 1. Tworzymy zapytanie SQL do pobrania zaszyfrowanego hasła dla danego użytkownika
        String querySQL = "SELECT password FROM accounts WHERE username = ?";

        try (
                // 2. Otwieramy połączenie z bazą danych
                Connection connection = databaseConnection.getSqlConnection();
                // 3. Przygotowujemy zapytanie SQL
                PreparedStatement statement = connection.prepareStatement(querySQL)
        ) {
            // 4. Ustawiamy wartość dla pierwszego parametru w zapytaniu (nazwa użytkownika)
                statement.setString(1, username);

            // 5. Wykonujemy zapytanie i pobieramy wyniki do ResultSet
            try (ResultSet resultSet = statement.executeQuery()) {
                // 6. Sprawdzamy, czy wynik zawiera dane (czy istnieje taki użytkownik)
                if (resultSet.next()) {
                    // 7. Pobieramy zaszyfrowane hasło z wyniku
                    String hashedPassword = resultSet.getString("password");

                    // 8. Weryfikujemy, czy podane hasło pasuje do zaszyfrowanego hasła
                    return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified;
                } else {
                    // 9. Jeśli użytkownik nie istnieje, zwracamy false
                    return false;
                }
            }
        } catch (SQLException e) {
            // 10. Jeśli wystąpi błąd SQL, rzucamy wyjątek RuntimeException
            throw new RuntimeException(e);
        }
    }
    public Account getAccount(String username){
        //stwórz zapytanie, aby znaleźć użytkownika na podstawie
        //jego nazwy
        String querySQL = "SELECT id, username FROM accounts WHERE username = ?";
        try (
        //nawiąż połączenie z bazą danych
        Connection connection = databaseConnection.getSqlConnection();
        //przygotowujemy zapytanie (statement) dla SQL
        PreparedStatement statement = connection.prepareStatement(querySQL);
        ){
            //ustawiamy wartość dla pierwszego parametru w zapytaniu
            statement.setString(1, username);
            //Wykonujemy zapytanie i pobieramy wyniki do ResultSet
            try(ResultSet resultSet = statement.executeQuery()){
                //Sprawdzamy, czy wynik zawiera dane (czy istnieje taki wynik)
                if(resultSet.next()){
                    // Pobieramy ID i nazwę użytkownika z wyniku
                    int id = resultSet.getInt("id");
                    String user = resultSet.getString("username");
                    //tworzymy i zwracamy obiekt Account
                    return new Account(id, user);
                } else {
                    //Jeśli użytkownik nie istnieje, zwracamy null
                    return null;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}