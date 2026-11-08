package com.mahesh.tracker.dao;

import com.mahesh.tracker.config.DatabaseConfig;
import com.mahesh.tracker.model.InterviewRound;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InterviewRoundDAO {

    private InterviewRound mapRound(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("interview_date");

        return new InterviewRound(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("round_name"),
                rs.getString("round_type"),
                timestamp == null ? null : timestamp.toLocalDateTime().toString(),
                rs.getString("status"),
                rs.getString("feedback")
        );
    }

    public List<InterviewRound> findAll() throws SQLException {
        String sql = """
                SELECT * FROM interview_rounds
                ORDER BY interview_date IS NULL, interview_date ASC, id DESC
                """;

        List<InterviewRound> rounds = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                rounds.add(mapRound(rs));
            }
        }

        return rounds;
    }

    public List<InterviewRound> findByCompanyId(long companyId)
            throws SQLException {

        String sql = """
                SELECT * FROM interview_rounds
                WHERE company_id = ?
                ORDER BY interview_date IS NULL, interview_date ASC, id DESC
                """;

        List<InterviewRound> rounds = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, companyId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rounds.add(mapRound(rs));
                }
            }
        }

        return rounds;
    }

    public Optional<InterviewRound> findById(long id)
            throws SQLException {

        String sql = "SELECT * FROM interview_rounds WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRound(rs));
                }
            }
        }

        return Optional.empty();
    }

    public InterviewRound create(InterviewRound round)
            throws SQLException {

        String sql = """
                INSERT INTO interview_rounds (
                    company_id, round_name, round_type,
                    interview_date, status, feedback
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, round.companyId());
            statement.setString(2, round.roundName());
            statement.setString(3, round.roundType());
            setInterviewDate(statement, 4, round.interviewDate());
            statement.setString(5, round.status());
            statement.setString(6, round.feedback());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }

        throw new SQLException("Creating interview round failed: no ID returned.");
    }

    public Optional<InterviewRound> update(long id, InterviewRound round)
            throws SQLException {

        String sql = """
                UPDATE interview_rounds
                SET company_id = ?,
                    round_name = ?,
                    round_type = ?,
                    interview_date = ?,
                    status = ?,
                    feedback = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, round.companyId());
            statement.setString(2, round.roundName());
            statement.setString(3, round.roundType());
            setInterviewDate(statement, 4, round.interviewDate());
            statement.setString(5, round.status());
            statement.setString(6, round.feedback());
            statement.setLong(7, id);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }

        return findById(id);
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM interview_rounds WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void setInterviewDate(
            PreparedStatement statement,
            int parameterIndex,
            String interviewDate
    ) throws SQLException {

        if (interviewDate == null || interviewDate.isBlank()) {
            statement.setNull(parameterIndex, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(
                    parameterIndex,
                    Timestamp.valueOf(interviewDate.replace("T", " "))
            );
        }
    }
}
