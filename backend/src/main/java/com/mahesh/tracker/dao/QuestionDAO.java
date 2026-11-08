package com.mahesh.tracker.dao;

import com.mahesh.tracker.config.DatabaseConfig;
import com.mahesh.tracker.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionDAO {

    private Question mapQuestion(ResultSet rs) throws SQLException {
        return new Question(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("question_text"),
                rs.getString("answer_notes"),
                rs.getString("difficulty"),
                rs.getBoolean("is_completed")
        );
    }

    public List<Question> findAll() throws SQLException {
        String sql = """
                SELECT * FROM questions
                ORDER BY created_at DESC, id DESC
                """;

        List<Question> questions = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                questions.add(mapQuestion(rs));
            }
        }

        return questions;
    }

    public Optional<Question> findById(long id) throws SQLException {
        String sql = "SELECT * FROM questions WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapQuestion(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Question create(Question question) throws SQLException {
        String sql = """
                INSERT INTO questions (
                    category, question_text, answer_notes,
                    difficulty, is_completed
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, question.category());
            statement.setString(2, question.questionText());
            statement.setString(3, question.answerNotes());
            statement.setString(4, question.difficulty());
            statement.setBoolean(5, Boolean.TRUE.equals(question.isCompleted()));

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }

        throw new SQLException("Creating question failed: no ID returned.");
    }

    public Optional<Question> update(long id, Question question)
            throws SQLException {

        String sql = """
                UPDATE questions
                SET category = ?,
                    question_text = ?,
                    answer_notes = ?,
                    difficulty = ?,
                    is_completed = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question.category());
            statement.setString(2, question.questionText());
            statement.setString(3, question.answerNotes());
            statement.setString(4, question.difficulty());
            statement.setBoolean(5, Boolean.TRUE.equals(question.isCompleted()));
            statement.setLong(6, id);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }

        return findById(id);
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM questions WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }
}
