-- ============================================================
--  StudentPortal - Full Schema (Drop & Recreate)
-- ============================================================
DROP DATABASE IF EXISTS StudentPortal;
CREATE DATABASE StudentPortal;
USE StudentPortal;

-- --------------------------------------------------------
-- Users
-- --------------------------------------------------------
CREATE TABLE Users (
    u_id             INT PRIMARY KEY AUTO_INCREMENT,
    name             VARCHAR(100)  NOT NULL,
    username         VARCHAR(50)   NOT NULL UNIQUE,
    password         VARCHAR(100)  NOT NULL,
    role             ENUM('Student','Professor','Admin') NOT NULL,
    current_semester INT           DEFAULT 1,
    semester_frozen  TINYINT(1)    DEFAULT 0   -- 1 = frozen
);

-- --------------------------------------------------------
-- Courses
-- --------------------------------------------------------
CREATE TABLE Courses (
    c_id              INT PRIMARY KEY AUTO_INCREMENT,
    course_name       VARCHAR(100) NOT NULL,
    prof_id           INT,
    offered_semester  INT          DEFAULT 1,
    FOREIGN KEY (prof_id) REFERENCES Users(u_id) ON DELETE SET NULL
);

-- --------------------------------------------------------
-- Enrollments  (student ↔ course)
-- --------------------------------------------------------
CREATE TABLE Enrollments (
    enroll_id  INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id  INT NOT NULL,
    semester   INT NOT NULL,
    withdrawn  TINYINT(1) DEFAULT 0,          -- 1 = withdrawn
    UNIQUE KEY uq_enroll (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Users(u_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES Courses(c_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- Attendance
-- --------------------------------------------------------
CREATE TABLE Attendance (
    a_id       INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT  NOT NULL,
    course_id  INT  NOT NULL,
    date       DATE NOT NULL,
    status     ENUM('P','A') NOT NULL,
    semester   INT,
    UNIQUE KEY uq_att (student_id, course_id, date),
    FOREIGN KEY (student_id) REFERENCES Users(u_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES Courses(c_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- Marks
-- --------------------------------------------------------
CREATE TABLE Marks (
    m_id       INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT    NOT NULL,
    course_id  INT    NOT NULL,
    mids       DOUBLE DEFAULT 0,
    sessionals DOUBLE DEFAULT 0,
    finals     DOUBLE DEFAULT 0,
    semester   INT,
    UNIQUE KEY uq_marks (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Users(u_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES Courses(c_id) ON DELETE CASCADE
);

-- ============================================================
--  Seed Data
-- ============================================================

-- Admin
INSERT INTO Users (name, username, password, role, current_semester)
VALUES ('System Admin', 'admin', 'admin123', 'Admin', 1);

-- Professors
INSERT INTO Users (name, username, password, role, current_semester)
VALUES ('Mr. Arshad Ali', 'arshadali', '@@##professor1', 'Professor', 1);

INSERT INTO Users (name, username, password, role, current_semester)
VALUES ('Ms. Sana Malik', 'sanamalik', '@@##professor2', 'Professor', 1);

-- Students  (semester 1)
INSERT INTO Users (name, username, password, role, current_semester)
VALUES ('Abdul Jaleel', 'abduljaleel', '@@##7850', 'Student', 1);

INSERT INTO Users (name, username, password, role, current_semester)
VALUES ('Sumeet Kumar', 'sumeet', '@@##1234', 'Student', 1);

-- Students  (semester 2)
INSERT INTO Users (name, username, password, role, current_semester)
VALUES ('Abdul Ahad', 'ahad', '@@##5678', 'Student', 2);

-- Courses  (semester 1 → arshadali u_id = 2)
INSERT INTO Courses (course_name, prof_id, offered_semester) VALUES ('ICT', 2, 1);
INSERT INTO Courses (course_name, prof_id, offered_semester) VALUES ('OOP', 2, 2);

-- Courses  (semester 2 → sanamalik u_id = 3)
INSERT INTO Courses (course_name, prof_id, offered_semester) VALUES ('Applied Physics', 3, 2);
INSERT INTO Courses (course_name, prof_id, offered_semester) VALUES ('Computer Networking', 3, 3);

-- Enroll semester-1 students in semester-1 courses
INSERT INTO Enrollments (student_id, course_id, semester) VALUES (4, 1, 1);
INSERT INTO Enrollments (student_id, course_id, semester) VALUES (4, 2, 1);
INSERT INTO Enrollments (student_id, course_id, semester) VALUES (5, 1, 1);
INSERT INTO Enrollments (student_id, course_id, semester) VALUES (5, 2, 1);

-- Enroll semester-2 student in semester-2 courses
INSERT INTO Enrollments (student_id, course_id, semester) VALUES (6, 3, 2);
INSERT INTO Enrollments (student_id, course_id, semester) VALUES (6, 4, 2);
