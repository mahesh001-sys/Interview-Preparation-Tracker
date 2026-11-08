CREATE DATABASE IF NOT EXISTS interview_tracker;
USE interview_tracker;

CREATE TABLE IF NOT EXISTS companies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_name VARCHAR(150) NOT NULL,
    job_role VARCHAR(150) NOT NULL,
    job_location VARCHAR(150),
    job_type VARCHAR(50),
    application_status VARCHAR(40) NOT NULL DEFAULT 'APPLIED',
    application_date DATE,
    job_description TEXT,
    website_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS interview_rounds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    round_name VARCHAR(100) NOT NULL,
    round_type VARCHAR(60),
    interview_date DATETIME,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_round_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(100) NOT NULL,
    question_text TEXT NOT NULL,
    answer_notes TEXT,
    difficulty VARCHAR(30) DEFAULT 'MEDIUM',
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS prep_notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_company_status ON companies(application_status);
CREATE INDEX idx_round_company ON interview_rounds(company_id);
CREATE INDEX idx_question_category ON questions(category);
