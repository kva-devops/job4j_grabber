package ru.job4j.grabber;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private final Connection cnn;

    public PsqlStore(Properties cfg) throws SQLException {
        try {
            Class.forName(cfg.getProperty("rabbit.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        this.cnn = DriverManager.getConnection(
                cfg.getProperty("rabbit.url"),
                cfg.getProperty("rabbit.username"),
                cfg.getProperty("rabbit.password")
        );
    }

    private void tableExistChecking() {
        try (Statement statement = cnn.createStatement()) {
            String sql =
                    "create table if not exists post (" + System.lineSeparator()
                            + "id serial primary key," + System.lineSeparator()
                            + "name varchar(255)," + System.lineSeparator()
                            + "text text," + System.lineSeparator()
                            + "link varchar(255) unique," + System.lineSeparator()
                            + "created timestamp" + System.lineSeparator()
                            + ");";
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(Post post) {
        tableExistChecking();
        try (PreparedStatement statement = cnn.prepareStatement(
                "insert into post (name, text, link, created) values (?, ?, ?, ?) on conflict (link) do nothing",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setObject(4, post.getCreated());
            statement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> result = new ArrayList<>();
        try (PreparedStatement statement = cnn.prepareStatement(
                "select * from post"
        )) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Post(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("link"),
                            resultSet.getString("text"),
                            resultSet.getTimestamp("created").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Post findById(int id) {
        Post result = new Post();
        try (PreparedStatement statement = cnn.prepareStatement(
                "select * from post where id = ?"
        )) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.setId(resultSet.getInt("id"));
                    result.setTitle(resultSet.getString("name"));
                    result.setLink(resultSet.getString("link"));
                    result.setDescription(resultSet.getString("text"));
                    result.setCreated(resultSet.getTimestamp("created").toLocalDateTime());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }
}