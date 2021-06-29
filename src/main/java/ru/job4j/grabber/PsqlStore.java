package ru.job4j.grabber;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public static void main(String[] args) {
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            Properties properties = new Properties();
            properties.load(in);
            PsqlStore parseSession = new PsqlStore(properties);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Post testPostOne = new Post(
                    1,
                    "Разработчик1",
                    "http://www.address1.ru",
                    "Описание вакансии разработчика1",
                    LocalDateTime.parse(LocalDateTime.now().format(formatter), formatter));
            Post testPostTwo = new Post(
                    2,
                    "Разработчик2",
                    "http://www.address2.ru",
                    "Описание вакансии разработчика2",
                    LocalDateTime.parse(LocalDateTime.now().format(formatter), formatter));
            parseSession.save(testPostOne);
            parseSession.save(testPostTwo);
            System.out.println(parseSession.getAll());
            System.out.println(parseSession.findById(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(Post post) {
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
