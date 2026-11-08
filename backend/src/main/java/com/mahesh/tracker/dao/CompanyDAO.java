package com.mahesh.tracker.dao;

import com.mahesh.tracker.config.DatabaseConfig;
import com.mahesh.tracker.model.Company;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompanyDAO {

    private Company mapCompany(ResultSet rs) throws SQLException {
        return new Company(
                rs.getLong("id"),
                rs.getString("company_name"),
                rs.getString("job_role"),
                rs.getString("job_location"),
                rs.getString("job_type"),
                rs.getString("application_status"),
                rs.getString("application_date"),
                rs.getString("job_description"),
                rs.getString("website_url")
        );
    }

    public List<Company> findAll() throws SQLException {
        String sql = "SELECT * FROM companies ORDER BY created_at DESC";
        List<Company> companies = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                companies.add(mapCompany(rs));
            }
        }

        return companies;
    }

    public Optional<Company> findById(long id) throws SQLException {
        String sql = "SELECT * FROM companies WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCompany(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Company create(Company company) throws SQLException {
        String sql = """
                INSERT INTO companies (
                    company_name, job_role, job_location, job_type,
                    application_status, application_date,
                    job_description, website_url
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, company.companyName());
            statement.setString(2, company.jobRole());
            statement.setString(3, company.jobLocation());
            statement.setString(4, company.jobType());
            statement.setString(5, company.applicationStatus());

            if (company.applicationDate() == null ||
                    company.applicationDate().isBlank()) {
                statement.setNull(6, Types.DATE);
            } else {
                statement.setDate(6, java.sql.Date.valueOf(company.applicationDate()));
            }

            statement.setString(7, company.jobDescription());
            statement.setString(8, company.websiteUrl());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }

        throw new SQLException("Creating company failed: no ID returned.");
    }

    public Optional<Company> update(long id, Company company)
            throws SQLException {

        String sql = """
                UPDATE companies
                SET company_name = ?,
                    job_role = ?,
                    job_location = ?,
                    job_type = ?,
                    application_status = ?,
                    application_date = ?,
                    job_description = ?,
                    website_url = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, company.companyName());
            statement.setString(2, company.jobRole());
            statement.setString(3, company.jobLocation());
            statement.setString(4, company.jobType());
            statement.setString(5, company.applicationStatus());

            if (company.applicationDate() == null ||
                    company.applicationDate().isBlank()) {
                statement.setNull(6, Types.DATE);
            } else {
                statement.setDate(6, java.sql.Date.valueOf(company.applicationDate()));
            }

            statement.setString(7, company.jobDescription());
            statement.setString(8, company.websiteUrl());
            statement.setLong(9, id);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }

        return findById(id);
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM companies WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }
}
